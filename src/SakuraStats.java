package sakurastats;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import static com.univocity.parsers.csv.UnescapedQuoteHandling.STOP_AT_CLOSING_QUOTE;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import static java.lang.Thread.sleep;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SakuraStats {
    static Connection conn;
    static Statement query;
    static int sleepyTime = 1;
    
    public static void main(String[] args) throws MalformedURLException, IOException, SQLException, InterruptedException, IllegalArgumentException, SecurityException, NoSuchFieldException, IllegalAccessException {
        System.setProperty("file.encoding","UTF-8");
        Field charset = Charset.class.getDeclaredField("defaultCharset");
        charset.setAccessible(true);
        charset.set(null,null);
        
        conn = DriverManager.getConnection("jdbc:sqlite:sakura.db");
        query = conn.createStatement();
        
        query.execute("PRAGMA main.cache_size = -100000");
        query.execute("PRAGMA locking_mode = EXCLUSIVE");
        query.execute("PRAGMA journal_mode = MEMORY");
        query.execute("PRAGMA synchronous = OFF");
        
        query.execute("CREATE TABLE IF NOT EXISTS player "
                + "(tag VARCHAR(255) PRIMARY KEY, name VARCHAR(255), "
                + "wins REAL, wars INT, card INT, dona REAL, rece REAL, "
                + "troph REAL, contr REAL, role REAL, vip REAL, times INT, "
                + "miss INT, isin INT)");
        query.execute("CREATE TABLE IF NOT EXISTS chest "
                + "(tag VARCHAR(255) PRIMARY KEY,"
                + "smc INT, lege INT, acti REAL)");
        query.execute("CREATE TABLE IF NOT EXISTS wardays "
                + "(day VARCHAR(255) PRIMARY KEY)");
        query.executeUpdate("UPDATE player SET isin = 0");

        if (args.length == 0) {
            List<String> clans = new ArrayList();
            File topClans = getPage("https://spy.deckshop.pro/top/gr/clans");
            Elements small = Jsoup.parse(topClans, "UTF-8").select(".text-muted");
            topClans.delete();
            
            for (Element muted : small) {
                String clan = muted.text().trim();
                if(clan.startsWith("#") && !clan.startsWith("# "))
                    clans.add(clan.replace("#", ""));
            }
            
            for (String clan : clans)
                readClan(clan);
            readMembers();
            
        } else {
            for (String arg : args)
                readClan(arg);
            readMembers(); 
        }
    } 
    
    static void readMembers() throws SQLException, IOException, InterruptedException {
        ResultSet res = query.executeQuery("SELECT * FROM player WHERE isin IS 1");
        
        int sec = 0;
        List tag = new ArrayList();
        System.out.print("\nRefreshing members... ");
        while(res.next()) {
            String player = res.getString("tag").replace("#","");
            System.out.print(player+" ");
            File file = getPage("https://statsroyale.com/profile/"+player+"/refresh");
            tag.add(player);
            
            sleep(sleepyTime);
            String feed = Jsoup.parse(file, "UTF-8").body().text();
            if (feed.split(":")[1].split(",")[0].equals("true")) {
                int newSec = Integer.valueOf(feed.split("\"secondsToUpdate\":")[1].replace("}",""));
                if (newSec > sec) sec = newSec;
            } file.delete();
        }
        
        sleep(sec);
        System.out.println("\n\nWaiting "+sec+"s...\n");
        for (Object pl : tag) {
            String player = (String) pl;
            File file = getPage("https://statsroyale.com/profile/"+player);
            Document page = Jsoup.parse(file, "UTF-8");
            file.delete();
            
            int lege = 1337;
            String counter = page.select(".chests__legendary")
                    .select(".chests__counter").text();
            if (counter.toLowerCase().equals("next")) lege = 0;
            else if (!counter.equals(""))
                lege = Integer.valueOf(counter);
            
            int smc = 1337;
            counter = page.select(".chests__super")
                    .select(".chests__counter").text();
            if (counter.toLowerCase().equals("next")) smc = 0;
            else if (!counter.equals(""))
                smc = Integer.valueOf(counter);
            
            file = getPage("https://statsroyale.com/profile/"+player+"/battles");
            page = Jsoup.parse(file, "UTF-8");
            file.delete();
            
            float min = 0;
            Elements replays = page.select(".replay__date");
            
            int games = 1;
            if (replays.size() != 0)
                games = replays.size();
            
            for (Element ele : replays) {
                String time = ele.text();
                if (time.contains("minute")) {
                    Matcher match = Pattern.compile("\\d+ minutes? ago").matcher(time);
                    while (match.find()) time = match.group(0);
                    min += Integer.valueOf(time.split(" ")[0]);
                } else if (time.contains("hour")) {
                    Matcher match = Pattern.compile("\\d+ hours? ago").matcher(time);
                    while (match.find()) time = match.group(0);
                    min += Integer.valueOf(time.split(" ")[0])*60;
                } else if (time.contains("day")) {
                    Matcher match = Pattern.compile("\\d+ days? ago").matcher(time);
                    while (match.find()) time = match.group(0);
                    min += Integer.valueOf(time.split(" ")[0])*60*24;
                } else if (time.contains("week")) {
                    Matcher match = Pattern.compile("\\d+ weeks? ago").matcher(time);
                    while (match.find()) time = match.group(0);
                    min += Integer.valueOf(time.split(" ")[0])*60*24*7;
                } else if (time.contains("month")) {
                    Matcher match = Pattern.compile("\\d+ months? ago").matcher(time);
                    while (match.find()) time = match.group(0);
                    min += Integer.valueOf(time.split(" ")[0])*60*24*30;
                } else System.out.println("NOT MATCHED - "+player+": "+time);
            }
            
            double acti = 1 - min/games/43200;
            System.out.println(player+":\t"+acti+",\t"+min/games);
            res = query.executeQuery("SELECT * FROM chest WHERE tag IS '#"+player+"'");
            
            if (!res.isBeforeFirst())
                query.executeUpdate("INSERT INTO chest VALUES ('#"
                        +player+"',1337,1337,0)");
            else
                query.executeUpdate("UPDATE chest SET lege = "+lege+","
                    + "smc = "+smc+",acti = "+acti+" WHERE tag IS '#"+player+"'");
        }
    }
    
    static void readClan(String clan) throws MalformedURLException, IOException, SQLException, SQLException, InterruptedException {
        File file = getPage("https://spy.deckshop.pro/clan/"+clan+"/csv");
        
//        Reader read = new FileReader(file,"UTF_8");
        CsvParserSettings settings = new CsvParserSettings();
        settings.setUnescapedQuoteHandling(STOP_AT_CLOSING_QUOTE);
        settings.getFormat().setDelimiter(',');
        settings.getFormat().setLineSeparator("\n");
        
        CsvParser parser = new CsvParser(settings);
        List<String[]> records = parser.parseAll(file, "UTF-8");
        file.delete();
        
        file = getPage("https://royaleapi.com/clan/"+clan+"/war/analytics/csv");   
        List<String[]> analytics = parser.parseAll(file, "UTF-8");
        file.delete();      
        
        
        List<String> wardays = new ArrayList();
        ResultSet res = query.executeQuery("SELECT * FROM wardays");
        while(res.next())
            wardays.add(res.getString(1));               
        
        Map<String,List<int[]>> pstat = new HashMap();
        List<int[]> pwars = new ArrayList();
        for (String[] war : analytics)
            if (!war[0].equals("name")) {
                for (int w = 0; w < (war.length-5)/4; w++)
                    if(war[4*w+5] != null &&
                            !wardays.contains(war[4*w+5]))
                        pwars.add(new int[] {
                            Integer.parseInt(war[4*w+8]),
                            Integer.parseInt(war[4*w+7]),
                            Integer.parseInt(war[4*w+6])});
                pstat.putIfAbsent("#"+war[1], pwars);
                pwars = new ArrayList();
            }
        
        String[] warday = new String[9];
        for (String[] war : analytics)
            if (!war[0].equals("name"))
                for (int w = 0; w < 9; w++)
                    if(w < (war.length-5)/4 
                            && war[4*w+5] != null)
                        warday[w] = war[4*w+5];
        
        res = query.executeQuery("SELECT MAX(dona),"
                + " MAX(rece), MAX(troph), MAX(contr), MAX(times),"
                + " MAX(CAST(wins AS float) / CAST(wars AS float)),"
                + " MAX(card/wars) FROM player");
        
        double maxDona = res.getDouble(1);
        double maxRece = res.getDouble(2);
        double maxTroph = res.getDouble(3);
        double maxContr = res.getDouble(4);
        double maxTimes = res.getInt(5);
        double maxWinR = res.getDouble(6);
        double maxCard = res.getInt(7);
        
        res = query.executeQuery("SELECT MIN(dona),"
                + " MIN(rece), MIN(troph), MIN(contr), MIN(times),"
                + " MIN(CAST(wins AS float) / CAST(wars AS float)),"
                + " MIN(card/wars) FROM player");
        
        double minDona = res.getDouble(1);
        double minRece = res.getDouble(2);
        double minTroph = res.getDouble(3);
        double minContr = res.getDouble(4);
        double minTimes = res.getInt(5);
        double minWinR = res.getDouble(6);
        double minCard = res.getInt(7);
                
        double pos = Double.MIN_VALUE;
        maxDona = maxDona - minDona + pos;
        maxRece = maxRece - minRece + pos;
        maxTroph = maxTroph - minTroph + pos;
        maxContr = maxContr - minContr + pos;
        maxTimes = maxTimes - minTimes + pos;
        maxWinR = maxWinR - minWinR + pos;
        maxCard = maxCard - minCard + pos;
        
        for (String[] record : records) 
            if (!record[0].equals("Rank")) {
                
                for (int i=0; i < record.length;i++)
                    System.out.print(record[i]+"  ");
                System.out.println("#"+clan);

                double chest = 0;
//                if (record[1] != null)
//                    chest = Integer.valueOf(record[1]);
                double dona = Integer.valueOf(record[1]);
                double rece = Integer.valueOf(record[2]);
                
                String tag = "";
                String name = "";
                double troph = 0;
                double contr = 0;
                double role = 0;
                int wins = 0;
                int wars = 0;
                int miss = 0;
                int card = 0;
                
                if (record.length == 9) {
                    tag = record[5];
                    name = record[4];
                    dona = Integer.valueOf(record[1]);
                    rece = Integer.valueOf(record[2]);
                    troph = Integer.valueOf(record[7]);
                    contr = Integer.valueOf(record[8]);
                    role = getRole(record[6]);       
                } else if (record.length == 8) {
                    tag = record[4].split(",\"")[1];
                    name = record[4].split(",\"")[0];
                   
                    troph = Integer.valueOf(record[6]);
                    contr = Integer.valueOf(record[7]);
                    role = getRole(record[5]);       
                } else {
                    System.out.println("ERROR!!!");
                    System.exit(1);
                }
                                
                List<int[]> stats = pstat.get(tag);
                if (stats != null)
                    for (int[] war : stats) {
                        card += war[2];
                        if (war[1]!=0) {
                            wins += war[0]/war[1];
                            wars++;
                        }
                        else miss++;
                }
                
                res = query.executeQuery("SELECT * FROM player WHERE tag IS '"+tag+"'");

                if (!res.isBeforeFirst()) {
                    PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO player VALUES ('"+tag+"',?,"+wins+","
                            +wars+","+card+","+dona+","+rece+","+troph+","
                            +contr+","+role+",-1,1,"+miss+",1)");
                    stmt.setString(1, StringEscapeUtils.escapeJava(name));
                    stmt.executeUpdate();
                    stmt.close();
                } else {
                    wins += res.getDouble("wins");
                    wars += res.getInt("wars");
                    card += res.getInt("card");
                    miss += res.getInt("miss");
                    
                    double oldDona = res.getDouble("dona");
                    double oldRece = res.getDouble("rece");
                    double oldTroph = res.getDouble("troph");
                    double oldContr = res.getDouble("contr");
                    double oldRole = res.getDouble("role");
                    int times = res.getInt("times");
                    
                    dona = (oldDona * times + dona) / (times + 1);
                    rece = (oldRece * times + rece) / (times + 1);
                    troph = (oldTroph * times + troph) / (times + 1);
                    contr = (oldContr * times + contr) / (times + 1);
                    role = (oldRole + role) / 2;
                    
                    double winR = (double)wins/(wars + pos);                    
                    double vip = 
                            .70 * (winR-minWinR)/maxWinR +
                            .02 * (dona-minDona)/maxDona +
                            .02 * (rece-minRece)/maxRece +
                            .06 * (troph-minTroph)/maxTroph +
                            .10 * (contr-minContr)/maxContr +
                            .05 * (times-minTimes)/maxTimes +                    
                            .05 * (role-.5)/.4;
                    
                    times++;                   
//                    System.out.println(
//                            "UPDATE player SET name = ?,dona = "+dona+",rece = "
//                            +rece+",troph = "+troph+",contr = "+contr+",role = "
//                            +role+",vip = "+vip+",wins = "+wins+",wars = "+wars+
//                            ",card = "+card+",times = "+times+
//                            ",isin = 1 WHERE tag IS '"+tag+"'");
                    
                    PreparedStatement stmt = conn.prepareStatement(
                            "UPDATE player SET name = ?,dona = "+dona+",rece = "
                            +rece+",troph = "+troph+",contr = "+contr+",role = "
                            +role+",vip = "+vip+",wins = "+wins+",wars = "+wars+
                            ",card = "+card+",times = "+times+
                            ",isin = 1 WHERE tag IS '"+tag+"'");
                    stmt.setString(1, StringEscapeUtils.escapeJava(name));
                    stmt.executeUpdate();
                    stmt.close();
                }
            }
        
        for (int w = 0; w < warday.length; w++)
            if (!wardays.contains(warday[w])
                    && warday[w] != null) {
//            System.out.println("INSERT OR REPLACE INTO wardays"
//                        + " VALUES ('"+warday[w]+"')");
                query.executeUpdate("INSERT OR REPLACE INTO wardays"
                        + " VALUES ('"+warday[w]+"')"); }
        System.out.println(Arrays.toString(warday));
    }
    
    static double getRole(String role) {
        if(role.equals("Leader")) return .9;
        if(role.equals("Co-leader")) return .9;
        if(role.equals("Elder")) return .6;
        return .5; 
    }
    
    static File getPage(String page) throws InterruptedException {
        try {
            File file = File.createTempFile("sakura.", ".tmp");
            URLConnection clanURL = new URL(page).openConnection();
            clanURL.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Mobile Safari/537.36");
            clanURL.connect();
            InputStream input = clanURL.getInputStream();
            Files.copy(input, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return file;
        } catch (Exception ex) {
            System.out.println("\nRetrying... "+page);
            sleep(sleepyTime++);
            return getPage(page);
        }
    }
}


//        Iterable<CSVRecord> recordss = CSVFormat.DEFAULT.withEscape(null).parse(read);
//        for (CSVRecord record : recordss) 
//            if (!record.get(0).equals("Rank")) {
//                
//                for (int i=0; i < record.size();i++)
//                    System.out.print(record.get(i)+"  ");
//                System.out.println("#"+clan);
//                        
//                double chest = 0;
//                if (!record.get(1).equals(""))
//                    chest = Integer.valueOf(record.get(1));
//                
//                String tag = record.get(6);
//                String name = record.get(5);
//                double dona = Integer.valueOf(record.get(2));
//                double rece = Integer.valueOf(record.get(3));
//                double troph = Integer.valueOf(record.get(8));
//                double contr = Integer.valueOf(record.get(9));
//                double role = getRole(record.get(7));
