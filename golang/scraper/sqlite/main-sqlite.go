package main

import (
	"database/sql"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"strings"

	// sqlite3 サードパーティーライブラリを導入する
	_ "github.com/mattn/go-sqlite3"
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

// ニュース記事を、全てSQLiteデータベースに投げる
func saveArticleDB(articles []article) {
	db, err := sql.Open("sqlite3", "./articles.db")
	if err != nil {
		panic(err)
	}
	_, err = db.Exec(
		`CREATE TABLE IF NOT EXISTS "articles" ("url" TEXT, "title" TEXT, "contents" TEXT)`,
	)
	if err != nil {
		panic(err)
	}
	defer db.Close()

	for _, article := range articles {
		_, err := db.Exec(`INSERT INTO articles (url, title, contents) VALUES (?, ?, ?)`, article.url, article.title, article.contents)
		if err != nil {
			panic(err)
		}
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
	b := `</header><div data-ual-view-type="detail"`
	e := `<div class="viewableWrap">`
	articles := make([]article, 0)
	for _, urlTitle := range urlTitles {
		p := get(urlTitle.url)
		contents, _ := substring(p, b, e, 0)
		article := article{
			url:      urlTitle.url,
			title:    urlTitle.title,
			contents: "<div " + strings.Replace(contents, "href", "invalidated", 0),
		}
		articles = append(articles, article)
		fmt.Printf("===========記事本文===========\n%s\n", contents)
	}
	return articles
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

// プログラム本文
// 　目次ページを取得
// 　目次ページから、リンクとタイトルが書かれている区画を全て抽出して、列挙
// 　リンクとタイトルが書かれている区画から、リンクとタイトルを抽出
// 　リンクとタイトルで、記事のページをダウンロードして、記事本文を抽出。ニュース記事で戻る
// 　ニュース記事を、HTMLファイルに追加保存
// 　ニュース記事を、全てSQLiteデータベースに投げる
func main() {
	// 例 "https://headlines.yahoo.co.jp/list/?m=kyodonews"
	indexURL := os.Args[1]
	// 目次ページを取得
	p := get(indexURL)
	// 目次ページから、リンクとタイトルが書かれている区画を全て抽出して、列挙
	linkBlocks := extractLinkBlock(p)
	// リンクとタイトルが書かれている区画から、リンクとタイトルを抽出
	urlTitles := extractURLTitle(linkBlocks)
	// リンクとタイトルで、記事のページをダウンロードして、記事本文を抽出。ニュース記事で戻る
	articles := getArticle(urlTitles)
	// ニュース記事を、HTMLファイルに追加保存
	saveArticle(articles)
	// ニュース記事を、全てSQLiteデータベースに投げる
	saveArticleDB(articles)
}
