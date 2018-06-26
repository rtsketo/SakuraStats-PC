<html>
<link rel="shortcut icon" href="favicon.ico" />
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Sakura Frontier</title>
    <meta name="viewport" content="width=device-width, initial-scale=1, minimum-scale=0.2, maximum-scale=1">
    <link rel="stylesheet" href="./Sakura Frontier_files/bootstrap.min.css">
    <script src="./Sakura Frontier_files/jquery.min.js.download"></script>
    <link rel="stylesheet" href="style.css">
<link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/v/bs4/dt-1.10.18/r-2.2.2/datatables.min.css"/>
 
<script type="text/javascript" src="https://cdn.datatables.net/v/bs4/dt-1.10.18/r-2.2.2/datatables.min.js"></script>
</head>

<body>
    <div class="navbar navbar-inverse navbar-fixedtop logo">
        <div class="container">
            <div class="row">
                <div class="col-xs-6 clana text-left align-middle">Sakura Frontier</div>
                <div class="col-xs-2 switch text-right align-middle">
                    <button class="btn-sm btn-danger" onclick="window.location.href='/'" style="color:beige"> Switch Stats </button>
                </div>
            </div>
        </div>
    </div>
    <div class="table-responsive stato">
    <table class="container table-condensed" id="QQmas">
<thead>
    <tr>
        <th>
            <h1>Member</h1></th>
        <th>
            <h1><center>LC</center></h1></th>
        <th>
            <h1><center>SMC</center></h1></th>
        <th>
            <h1><center>Importance</center></h1></th>
        <th>
            <h1><center>Activity</center></h1></th>
    </tr>
</thead>
<tbody class="switcho">
    <?php
$db = new SQLite3('sakura.db');

$results = $db->query('SELECT name, lege, smc, acti, vip, chest.tag FROM chest, player WHERE chest.tag = player.tag AND isin = 1 ORDER BY acti DESC');
while ($row = $results->fetchArray(SQLITE3_ASSOC)) {
    echo '<tr class="clickable-row" data-href="https://spy.deckshop.pro/player/'.str_replace('#','',$row['tag']).'">';
    echo '<td>'.decode($row['name']).'</td>';
	echo '<td><center><ooy>+</ooy>'.$row['lege'].'</center></td>';
	echo '<td><center><ooy>+</ooy>'.$row['smc'].'</center></td>';
    if ($row['vip']==-1) 
        echo '<td><center>new</center></td>';
    else
        echo '<td><center>'.round($row['vip']*100).'<ooy>%</ooy></center></td>';
    echo '<td><center>'.round($row['acti']*100).'<ooy>%</ooy></center></td>';
    echo '</tr>';
}
			
			function decode($payload) {
				return array_pop(json_decode('["'.$payload.'"]'));
			}
			
			?>
</tbody>
</table>
        </div>
</body>
<script>
    
    $(document).ready(function() {
        $('#QQmas').DataTable();
    } );
    
    $('#QQmas').DataTable( {
        "paging": false,
        "pageLength": 50,
        "pagingType": "simple_numbers",
        "searching": false
    } );
    
    jQuery(document).ready(function ($) {
        $(".clickable-row").click(function () {
            window.open($(this).data("href"),'_blank');
        });
    });
</script>

</html>