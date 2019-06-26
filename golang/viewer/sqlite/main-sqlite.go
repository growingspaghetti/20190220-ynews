package main

import (
	"bytes"
	"database/sql"
	"fmt"
	"net/http"

	_ "github.com/mattn/go-sqlite3"
)

// データ型宣言
// ■unt リンクとタイトルとは
// 　・url   文字列
// 　・title 文字列
type unt struct {
	url, title string
}

// 変数宣言
// ■db データベースとはsql.DB
var (
	db *sql.DB
)

// URLから記事本文を検索して戻る
func cat(url string) string {
	var c string
	row := db.QueryRow(`SELECT contents FROM articles WHERE url=?;`, url)
	err := row.Scan(&c)
	if err != nil {
		panic(err)
	}
	return c
}

// 記事本文ページ生成手は、フォームバリューよりその記事のURLを受け取り、記事本文を返す
func cathandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, cat(r.FormValue("url")))
}

// データベースにある記事を列挙して、その目次とタイトルで戻る
func ls() []unt {
	rows, err := db.Query(`SELECT url,title FROM articles;`)
	if err != nil {
		panic(err)
	}
	defer rows.Close()

	unts := []unt{}
	for rows.Next() {
		var r unt
		if err := rows.Scan(&r.url, &r.title); err != nil {
			panic(err)
		}
		unts = append(unts, r)
	}
	return unts
}

// 記事一覧ページ生成手は記事一覧をページを生成して返す
func lshandler(w http.ResponseWriter, r *http.Request) {
	unts := ls()
	var buf bytes.Buffer
	for _, unt := range unts {
		buf.WriteString("<li>")
		buf.WriteString("<a href='javascript:void(0);' url='")
		buf.WriteString(unt.url)
		buf.WriteString("'>")
		buf.WriteString(unt.title)
		buf.WriteString("</a>")
		buf.WriteString("</li>\n")
	}
	fmt.Fprintf(w, `    
    <!DOCTYPE html>
    <html>
    <head>
        <style>
        ul#headers                   {padding:4px 0px 4px 4px;}
        ul#headers a                 {display:block; padding:4px;}
        ul#headers a:focus           {background-color:yellow;}
        ul#headers li                {background: white; list-style-type: none;}
        ul#headers li:nth-child(odd) {background: #ececec; }
        div#article                  {padding:10px;}
        .split, .gutter.gutter-horizontal {
            float: left;
        }
        .gutter.gutter-horizontal {
            cursor: ew-resize;
            background-color: grey;
        }
        </style>
        <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/split.js/1.5.9/split.min.js"></script>
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.0/css/bootstrap.min.css">
    </head>
    <body>
        <div>
            <div class="split" id="one" style="overflow:auto;">
                <ul id='headers'>`+buf.String()+`</ul>
            </div>
            <div class="split" id="two" style="overflow:auto;">
                <div id='article'></div>
            </div>
        </div>
        <script>
        $('document').ready(function(){
            var h = $(window).height();
            $('#one').height(h);
            $('.gutter.gutter-horizontal').height(h);
        });
        $(window).resize(function() {
            var h = $(window).height();
            console.log(h);
            $('#one').height(h);
            $('#two').height(h);
            $('.gutter.gutter-horizontal').height(h);
        });
        $(function() {
            $('ul#headers a').focusin(function(e) {
                var u = $(this).attr('url');
                console.log(u);
                $.ajax({
                    url:'/cat',
                    type:'GET',
                    data:{
                        'url': u
                    }
                })
                .done((data) => {
                    var ttl = $(this).text();
                    $('div#article').html("<h1>" + ttl + "</h1>" + data);
                    $('#two').height($(window).height());
                })
                .fail((data) => {
                    $(this).css('background-color', 'pink');
                })
                .always((data) => {
                    console.log(data);
                });
            });
        });
        Split(['#one', '#two'], {
            gutterSize: 4,
        });
        </script>
    </body>
    </html>`)
}

// 記事一覧転送手は記事一覧へ転送する
func fwdhandler(w http.ResponseWriter, r *http.Request) {
	http.Redirect(w, r, "/ls", http.StatusFound)
}

// プログラム本文
// 　データベース開く
// 　ルートディレクトリページが開かれたら記事一覧転送手へ送る
// 　記事一覧ページが開かれたら記事一覧ページ生成手へ送る
// 　記事本文ページが開かれたら記事本文ページ生成手へ送る
// 　サーバーを起動
func main() {
	// データベース開く
	db, _ = sql.Open("sqlite3", "./articles.db")
	defer db.Close()
	// ルートディレクトリページが開かれたら記事一覧転送手へ送る
	http.HandleFunc("/", fwdhandler)
	// 記事一覧ページが開かれたら記事一覧ページ生成手へ送る
	http.HandleFunc("/ls", lshandler)
	// 記事本文ページが開かれたら記事本文ページ生成手へ送る
	http.HandleFunc("/cat", cathandler)
	// サーバーを起動
	http.ListenAndServe(":8080", nil)
}
