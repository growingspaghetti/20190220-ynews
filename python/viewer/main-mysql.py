import sys
import os

from PyQt5.QtWidgets import *
from PyQt5.QtCore import *
from PyQt5.QtWebEngineWidgets import QWebEngineView
from PyQt5.QtGui import *
from contextlib import closing
import mysql.connector
from mysql.connector import pooling

# ■QStyledItemDelegateとは
# 　・値
# 　・displayTextする〜
# 　　各個適宜定義
#
# ItemDelegateとはQStyledItemDelegate
# 　そのdisplayTextした時は〜
# 　　自身の値【1】で戻る
class ItemDelegate(QStyledItemDelegate):
    def displayText(self, value, locale):
        return value[1]

# AppとはQWidget
# 　その初期化する時は〜
# 　そのユーザーインターフェイスを初期化する時は〜
# 　その記事本文をデータベースから取り出す（URLの）時は〜
# 　そのブラウザに記事をセットする時は〜
# 　そのリストを初期化する時は〜
# 　
class App(QWidget):

    # 初期化
    def __init__(self):
        self.connection_pool = mysql.connector.pooling.MySQLConnectionPool(
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
        super().__init__()
        self.browser    = QWebEngineView()
        self.model      = QStandardItemModel()
        self.listWidget = QListView()
        self.init_ui()

    # ユーザーインターフェイスを初期化する
    def init_ui(self):
        self.listWidget.setModel(self.model)
        self.listWidget.setAlternatingRowColors(True)
        self.listWidget.setEditTriggers(QAbstractItemView.NoEditTriggers)
        self.listWidget.setSelectionMode(
            QAbstractItemView.SingleSelection
        )
        self.listWidget.selectionModel().selectionChanged.connect(self.load_html)
        self.listWidget.setItemDelegate(ItemDelegate(self.listWidget))
        grid = QGridLayout()
        grid.setSpacing(10)
        splitter = QSplitter(Qt.Horizontal)
        splitter.addWidget(self.listWidget)
        splitter.addWidget(self.browser)
        grid.addWidget(splitter,1,0)
        self.init_list_items()
        self.setLayout(grid)
        self.resize(1200, 800)
        self.listWidget.setMinimumWidth(600)
        self.browser.setMinimumWidth(600)
        self.show()

    # 記事本文をデータベースから取り出す（URLの）
    def fetch_contents(self, url):
        with closing(self.connection_pool.get_connection()) as conn:
            c = conn.cursor()
            c.execute("SELECT contents FROM articles WHERE url=%s;", [url])
            rows = c.fetchall()
            for row in rows:
                return row[0]

    # ブラウザに記事をセットする
    def load_html(self):
        for i in self.listWidget.selectedIndexes():
            html = '<h1>' + i.data()[1] + '</h1>' + self.fetch_contents(i.data()[0])
            self.browser.setHtml(html)

    # リストを初期化する
    def init_list_items(self):
        with closing(self.connection_pool.get_connection()) as conn:
            c = conn.cursor()
            c.execute("SELECT url,title FROM articles;")
            rows = c.fetchall()
            for row in rows:
                item = QStandardItem()
                item.setData(row, Qt.DisplayRole)
                item.setSizeHint(QSize(20, 30))
                self.model.appendRow(item)

# プログラム本文
# 　以下、QTアプリケーションを開始する
# 　QWidgetたるAppを作成して
# 　それを実行
if __name__ == '__main__':
    app = QApplication(sys.argv)
    ex = App()
    sys.exit(app.exec_())