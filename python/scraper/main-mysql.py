import os
import requests
import mysql.connector
from contextlib import closing
from mysql.connector import pooling


# ●範囲切り取り　（文章を　開始文字　と　終了文字　で)
# その範囲と検索終了地点で戻る　例：「ABCDE」を「A」と「CD」で範囲切り取り　⇒　Bと4で戻る
# str, begin, end, offset. returns ("",-1) if not found. (between, next offset to pass)
def substring(s, b, e, o):
    ib = s.find(b, o)
    if ib == -1:
        return "", -1
    ie = s.find(e, ib+len(b))
    if ie == -1:
        return "", -1
    return s[ib+len(b):ie], ie+len(e)

# ●範囲切り取りを繰り返して全て取り出し、列挙
# returns substringsBetween
def substrings(s, b, e, o, buf):
    ib = s.find(b, o)
    if ib == -1:
        return buf
    ie = s.find(e, ib+len(b))
    if ie == -1:
        return buf
    buf.append(s[ib+len(b):ie])
    return substrings(s, b, e, ie+len(e), buf)

# 目次ページから、リンクとタイトルが書かれている区画を全て抽出して、列挙
def extract_link_block(index_page):
    link_blocks = substrings(index_page, '<li class="listFeedWrap', '<!--/.listFeedWrapCont-->', 0, [])
    print("＝＝＝リンクブロック＝＝＝\n" + ', '.join(link_blocks))
    return link_blocks

# リンクとタイトルが書かれている区画から、リンクとタイトルを抽出
def extract_url_title(link_blocks):
    urls_titles = []
    for link_block in link_blocks:
        url = substring(link_block, '<a href="'    , '"'   , 0     )
        ttl = substring(link_block, 'class="titl">', '</dt', url[1])
        urls_titles.append((url[0],ttl[0]))
        print("＝＝＝リンクとタイトル＝＝＝\n" + url[0] + "\t" + ttl[0])
    return urls_titles

# リンクとタイトルで、記事のページをダウンロードして、記事本文を抽出。ニュース記事で戻る
def get_article(urls_titles):
    articles = []
    for url_title in urls_titles:
        article_page = requests.get(url_title[0]).text
        txt = substring(article_page, '<div class="paragraph">', '<!-- /.ynDetailText -->', 0)
        articles.append((url_title[0], url_title[1], txt[0]))
        print("＝＝＝記事本文＝＝＝\n" + txt[0])
    return articles

# ニュース記事を、HTMLファイルに追加保存
def save_articles(articles):
    with open('ヤフー記事.html', 'a') as f:
        for article in articles:
            f.write("<hr>\n<h1>"+ article[1] + "</h1>\n" + article[2] + "\n")

# ニュース記事を、全てMySQLデーテベースに投げる
def save_articles_mysql(articles):
    connection_pool = mysql.connector.pooling.MySQLConnectionPool(
        pool_name="pynative_pool",
        pool_size=1,
        pool_reset_session=True,
        host='news-recorder.cnfoeuugi3ju.ap-northeast-1.rds.amazonaws.com',
        database='newsrecorder',
        user='root',
        password=os.environ['NEWS_PASS'],
        ssl_ca='rds-combined-ca-bundle.pem',
        charset='utf8'
    )
    with closing(connection_pool.get_connection()) as conn:
        c = conn.cursor()
        c.executemany('INSERT IGNORE INTO articles (url, title, contents) values (%s,%s,%s)', articles)
        conn.commit()


###################################################
# ■目次ページとは
# 　・リンクとタイトルが書かれている区画
# 　　・リンク
# 　　・タイトル
# 　・リンクとタイトルが書かれている区画
# 　　・リンク
# 　　・タイトル
# 　・リンクとタイトルが書かれている区画
# 　　・リンク
# 　　・タイトル
###################################################

###################################################
# ■ニュース記事とは
# 　・URL
# 　・タイトル
# 　・本文
###################################################

# 目次ページを取得
response = requests.get('https://headlines.yahoo.co.jp/list/?m=kyodonews').text
# 目次ページから、リンクとタイトルが書かれている区画を全て抽出して、列挙
link_blocks = extract_link_block(response)
# リンクとタイトルが書かれている区画から、リンクとタイトルを抽出
urls_titles = extract_url_title(link_blocks)
# リンクとタイトルで、記事のページをダウンロードして、記事本文を抽出。ニュース記事で戻る
articles    = get_article(urls_titles)
# ニュース記事を、HTMLファイルに追加保存
save_articles(articles)
# ニュース記事を、全てMySQLデーテベースに投げる
save_articles_mysql(articles)