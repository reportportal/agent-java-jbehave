Scenario: Stock trade alert

Given a stock of symbol <symbol> and a threshold <threshold>
When the stock is traded at price <price>
Then the alert status should be status <status>
When I have first parameter <symbol> and second parameter <symbol>

Examples:
|symbol|threshold|price|status|
|STK1$|10.0|5.0|OFF|
|STK1$|10.0|11.0|ON|
