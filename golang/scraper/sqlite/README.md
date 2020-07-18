<pre><font color="#A6E22E"><b>ryoji@ubuntu</b></font>:<font color="#66D9EF"><b>~/ynews</b></font>$ mkdir data
<font color="#A6E22E"><b>ryoji@ubuntu</b></font>:<font color="#66D9EF"><b>~/ynews</b></font>$ chown 1000:1000 data
<font color="#A6E22E"><b>ryoji@ubuntu</b></font>:<font color="#66D9EF"><b>~/ynews</b></font>$ docker run -v &quot;$(pwd)/data:/app/data&quot; -u 1000:1000 ryojikodakari/ynews-mini-scraper-20200718 https://headlines.yahoo.co.jp/list/?m=kyodonews
</pre>

![](./img/files.png)

![](./img/html.png)

----

<pre><font color="#A6E22E"><b>ryoji@ubuntu</b></font>:<font color="#66D9EF"><b>/media/dev/20190220-ynews/golang/scraper/sqlite</b></font>$ go mod init github.com/growingspaghetti/20190220-ynews/golang/scraper/sqlite
go: creating new go.mod: module github.com/growingspaghetti/20190220-ynews/golang/scraper/sqlite
<font color="#A6E22E"><b>ryoji@ubuntu</b></font>:<font color="#66D9EF"><b>/media/dev/20190220-ynews/golang/scraper/sqlite</b></font>$ go build
go: finding module for package github.com/mattn/go-sqlite3
go: found github.com/mattn/go-sqlite3 in github.com/mattn/go-sqlite3 v1.14.0</pre>
