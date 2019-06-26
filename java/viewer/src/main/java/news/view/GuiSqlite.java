package news.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;


// Guiとはフレーム部品(ウィンドウ部品)
// 　・パネル部品
// 　・リスト内容
// 　・リスト部品
// 　・ブラウザ部品
// 　その初期化する時は〜
// 　そのリスト内容を初期化する時は〜
// 　そのリストの選択に変更があった時は〜
// 　記事本文をデータベースから取り出す（URLの）時は〜
//
public class GuiSqlite extends JFrame {
    private final JFXPanel                   fxContainer = new JFXPanel();
    private final DefaultListModel<TitleUrl> model       = new DefaultListModel<>();
    private final JList<TitleUrl>            list        = new JList<>(model);
    private       WebView                    webView;

    // データ型宣言
    // ■UrlTitle
    // 　・url
    // 　・title
    // 　文字として表示する時は〜
    private class TitleUrl {
        private final String title;
        private final String url;
        public TitleUrl(String title, String url) {
            this.title = title;
            this.url   = url;
        }
        public String getTitle() {
            return title;
        }
        public String getUrl() {
            return url;
        }
        @Override
        public String toString() {
            return title;
        }
    }

    // データベース補助コード
    private Connection getConnection() throws Exception {
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection("jdbc:sqlite:articles.db");
    }

    // データベース補助コード
    private void prepareDatabase() throws Exception {
        try (Statement stmt = getConnection().createStatement();) {
            stmt.execute("CREATE TABLE IF NOT EXISTS articles (url TEXT, title TEXT, contents TEXT)");
        }
    }
    
    // 記事本文をデータベースから取り出す（URLの）
    private void getContents(TitleUrl tu) {
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs   = stmt.executeQuery("SELECT contents FROM articles WHERE url='" + tu.getUrl() + "';");) {
            if (rs.next()) {
                String c = rs.getString("contents");
                Platform.runLater(() -> webView.getEngine().loadContent("<h1>" + tu.getTitle() + "</h1>" + c));
            }
        } catch (Exception ex) {
            Logger.getLogger(GuiSqlite.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    // リストの選択に変更があった時は
    private void applySelectionListener() {
        list.addListSelectionListener((ListSelectionEvent e) -> {
            TitleUrl tu = list.getSelectedValue();
            getContents(tu);
        });
    }

    // 初期化補助コード
    private void createScene() {
        webView   = new WebView();
        VBox vBox = new VBox(webView);
        fxContainer.setScene(new Scene(vBox));
    }

    // リスト内容を初期化する
    private void initList() throws Exception {
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs   = stmt.executeQuery("SELECT url,title FROM articles;");) {
            while (rs.next()) {
                String u = rs.getString("url");
                String t = rs.getString("title");
                model.addElement(new TitleUrl(t, u));
            }
        }
    }

    // リスト部品設定補助コード
    private class MyListCellThing extends JLabel implements ListCellRenderer<Object> {
        private static final long serialVersionUID = 1L;
        public MyListCellThing() {
            setOpaque(true);
        }
        @SuppressWarnings("rawtypes")
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value.toString());
            if (isSelected) {
                setBackground(Color.YELLOW);
            } else if (index % 2 == 0) {
                setBackground(new Color(246, 246, 246));
            } else {
                setBackground(Color.WHITE);
            }
            this.setPreferredSize(new Dimension(0, 24));
            return this;
        }
    }

    // 初期化する
    private void init() {
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new MyListCellThing());
        JSplitPane splitPane
            = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(list), fxContainer);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(300);
        add(splitPane, BorderLayout.CENTER);
        Platform.runLater(this ::createScene);
        try {
            prepareDatabase();
            initList();
        } catch (Exception ex) {
            Logger.getLogger(GuiSqlite.class.getName()).log(Level.SEVERE, null, ex);
        }
        applySelectionListener();
    }

    // ■フレーム部品
    //　 ×ボタンが押された時は〜
    // 　　各個適宜定義
    //　 初期化時は〜
    // 　　各個適宜定義
    // 　・サイズ
    // 　・表示位置
    // 　・表示非表示
    // 
    //
    // Guiとはフレーム部品
    // 　×ボタンが押された時は〜
    // 　　終了
    // 　初期化時は〜
    // 　　リストを初期化したり諸々せよ
    // 　サイズは900x500
    // 　表示位置は画面真ん中
    // 　表示非表示は表示
    //
    //
    // プログラム本文
    // 　フレーム部品たるGuiを作成して
    // 　それを初期化
    // 　表示設定を表示に
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GuiSqlite frame = new GuiSqlite();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.init();
            frame.setPreferredSize(new Dimension(900, 500));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
