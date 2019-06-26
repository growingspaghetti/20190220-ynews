package main

import (
	"crypto/tls"
	"crypto/x509"
	"database/sql"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"strings"

	// mysql サードパーティーライブラリを導入する
	"github.com/go-sql-driver/mysql"
)

var (
	db *sql.DB
)

// データ型宣言
// ■UrlTitle リンクとタイトルとは
// 　・url   文字列
// 　・title 文字列
type urlTitle struct {
	url, title string
}

// データ型宣言
// ■article ニュース記事とは
// 　・url      文字列
// 　・title    文字列
// 　・contents 文字列
type article struct {
	url, title, contents string
}

// ●範囲切り取りを繰り返して全て取り出し、列挙
func substrings(str, b, e string, o int, buf []string) []string {
	ib := strings.Index(str[o:], b)
	if ib == -1 {
		return buf
	}
	o += ib + len(b)
	ie := strings.Index(str[o:], e)
	if ie == -1 {
		return buf
	}
	buf = append(buf, str[o:o+ie])
	return substrings(str, b, e, o+ie+len(e), buf)
}

// ●範囲切り取り　（文章を　開始文字　と　終了文字　で) 検索開始地点はo。
// その範囲と検索終了地点で戻る。
// 例：「ABCDE」を「A」と「CD」で範囲切り取り　⇒　Bと4で戻る
func substring(str, b, e string, o int) (string, int) {
	ib := strings.Index(str[o:], b)
	if ib == -1 {
		return "", -1
	}
	o += ib + len(b)
	ie := strings.Index(str[o:], e)
	if ie == -1 {
		return "", -1
	}
	return str[o : o+ie], o + ie + len(e)
}

// ●HTTPデータ取得　(URLの)
func get(url string) string {
	resp, err := http.Get(url)
	if err != nil {
		panic(err)
	}
	defer resp.Body.Close()
	page, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		panic(err)
	}
	return string(page[:])
}

// ニュース記事を、全てMySQLデータベースに投げる
func saveArticleDB(articles []article) {
	if len(articles) == 0 {
		return
	}
	qs := make([]string, 0)
	vs := make([]interface{}, 0)
	for _, article := range articles {
		qs = append(qs, "(?, ?, ?)")
		vs = append(vs, []interface{}{article.url, article.title, article.contents}...)
	}
	_, err := db.Exec(`INSERT IGNORE INTO articles (url, title, contents) VALUES `+strings.Join(qs, ","), vs...)
	if err != nil {
		panic(err)
	}
}

// ニュース記事を、HTMLファイルに追加保存
func saveArticle(articles []article) {
	f, err := os.OpenFile("articles.html", os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0600)
	if err != nil {
		panic(err)
	}
	defer f.Close()

	for _, article := range articles {
		if _, err = f.WriteString("<hr>\n<h1>" + article.title + "</h1>\n" + article.contents); err != nil {
			panic(err)
		}
	}
}

// リンクとタイトルで、記事のページをダウンロードして、記事本文を抽出。ニュース記事で戻る
func getArticle(urlTitles []urlTitle) []article {
	b := `<div class="paragraph">`
	e := `<!-- /.ynDetailText -->`
	articles := make([]article, 0)
	for _, urlTitle := range urlTitles {
		p := get(urlTitle.url)
		contents, _ := substring(p, b, e, 0)
		article := article{
			url:      urlTitle.url,
			title:    urlTitle.title,
			contents: contents,
		}
		articles = append(articles, article)
		fmt.Printf("===========記事本文===========\n%s\n", contents)
	}
	return articles
}

// リンクとタイトルの中ですでに取得されているものを除いて、それで戻る
func filter(urlTitles []urlTitle) []urlTitle {
	qs := make([]string, 0)
	us := make([]interface{}, 0)
	for _, urlTitle := range urlTitles {
		qs = append(qs, "?")
		us = append(us, urlTitle.url)
	}
	rows, err := db.Query(`SELECT url FROM articles WHERE url IN (`+strings.Join(qs, ",")+`);`, us...)
	if err != nil {
		panic(err)
	}
	defer rows.Close()

	dups := make(map[string]struct{}, 0)
	for rows.Next() {
		var r string
		if err := rows.Scan(&r); err != nil {
			panic(err)
		}
		dups[r] = struct{}{}
	}

	filtered := make([]urlTitle, 0)
	for _, urlTitle := range urlTitles {
		if _, ok := dups[urlTitle.url]; !ok {
			filtered = append(filtered, urlTitle)
		}
	}
	return filtered
}

// リンクとタイトルが書かれている区画から、リンクとタイトルを抽出
func extractURLTitle(linkBlocks []string) []urlTitle {
	urlTitles := make([]urlTitle, 0)
	for _, linkBlock := range linkBlocks {
		url, e := substring(linkBlock, `href="`, `"`, 0)
		ttl, _ := substring(linkBlock, `<div class="newsFeed_item_title">`, `</div`, e)
		urlTitle := urlTitle{url: url, title: ttl}
		fmt.Printf("===========URLとタイトル===========\n%s\n", urlTitle)
		urlTitles = append(urlTitles, urlTitle)
	}
	return urlTitles
}

// 目次ページから、リンクとタイトルが書かれている区画を全て抽出して、列挙
func extractLinkBlock(indexPage string) []string {
	b := `<li class="newsFeed_item ">`
	e := `</li>`
	linkBlocks := substrings(indexPage, b, e, 0, make([]string, 0))
	fmt.Printf("===========linkblocks===========\n%s\n", linkBlocks)
	return linkBlocks
}

// データベース補助コード
// ●データベース開く
func opendb() *sql.DB {
	rds := strings.Join([]string{
		"root:",
		os.Getenv("NEWS_PASS"),
		"@tcp",
		"(newsrecorder.czcdhip1tz6r.eu-north-1.rds.amazonaws.com:3306)",
		"/newsrecorder",
		"?tls=custom&parseTime=true&charset=utfmb4,utf8",
	}, "")
	ca := x509.NewCertPool()
	pem, _ := ioutil.ReadFile("rds-combined-ca-bundle.pem")
	ca.AppendCertsFromPEM(pem)
	mysql.RegisterTLSConfig("custom", &tls.Config{RootCAs: ca})
	db, _ := sql.Open("mysql", rds)
	return db
}

// プログラム本文
// 　データベース開く
// 　引数１を目次ページURLに代入
// 　目次ページを取得
// 　目次ページから、リンクとタイトルが書かれている区画を全て抽出して、列挙
// 　リンクとタイトルが書かれている区画から、リンクとタイトルを抽出
// 　リンクとタイトルの中ですでに取得されているものを除いて、それで戻る
// 　リンクとタイトルで、記事のページをダウンロードして、記事本文を抽出。ニュース記事で戻る
// 　ニュース記事を、HTMLファイルに追加保存
// 　ニュース記事を、全てMySQLデータベースに投げる
//
// [使い方]
//　./main-mysql https://headlines.yahoo.co.jp/list/?m=jij
//
func main() {
	// データベース開く
	db = opendb()
	defer db.Close()
	// 引数１を目次ページURLに代入
	indexURL := os.Args[1]
	// 目次ページを取得
	p := get(indexURL)
	// 目次ページから、リンクとタイトルが書かれている区画を全て抽出して、列挙
	linkBlocks := extractLinkBlock(p)
	// リンクとタイトルが書かれている区画から、リンクとタイトルを抽出
	urlTitles := extractURLTitle(linkBlocks)
	// リンクとタイトルの中ですでに取得されているものを除いて、それで戻る
	urlTitles = filter(urlTitles)
	// リンクとタイトルで、記事のページをダウンロードして、記事本文を抽出。ニュース記事で戻る
	articles := getArticle(urlTitles)
	// ニュース記事を、HTMLファイルに追加保存
	saveArticle(articles)
	// ニュース記事を、全てMySQLデータベースに投げる
	saveArticleDB(articles)
}
