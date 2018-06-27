# SakuraStats

## Site Example

http://rtsketo.duckdns.org



## Usage
##### To avoid missing a war, the app should run at least once in 18 days.
1) Make sure the files are in the proper directory (see below).

2) Run the app with this command, adding your clans' hashtags:

        java -jar SakuraStats.jar <clantag1> <clantag2> ... <clantagN>

  *Example*: java -jar SakuraStats.jar 2YCJRUC 209LRG2P




## Files

src: Application's source code.<br>
bin: Compiled JAR. The files need to be in application's root folder.<br>
lib: Dependencies. The whole folder needs to be in application's root folder.<br>
site: Example site.<br>



### File structure:

bin<br>
-- lib<br>
----- jsoup-1.11.2.jar<br>
----- commons-io-2.6.jar<br>
----- sqlite-jdbc-3.20.0.jar<br>
----- commons-text-1.2.jar<br>
----- commons-lang3-3.7.jar<br>
----- univocity-parsers-2.5.9.jar<br>
-- SakuraStats.jar<br>
-- SakuraStats.bat<br>
