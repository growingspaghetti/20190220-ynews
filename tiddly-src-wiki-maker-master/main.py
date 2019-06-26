import glob
import os


def read_text(path, encoding):
    with open(path, 'r', encoding=encoding) as f: # r read-only 読み込み専用モードで開く
        return f.read()


def write_text(path, text):
    with open(path, 'w', encoding="utf-8") as f:
        f.write(text)


def get_unique_title(title, set):
    if not title in set:
        set.add(title)
        return title
    else:
        counter = 2
        while True:
            unique_title = title + "_" + str(counter) # 整数型を文字列型へ変換
            if not unique_title in set:
                set.add(unique_title)
                return unique_title
            else:
                counter += 1


class TiddlySrcMaker:
    def __init__(self, path, extension, postfix, book_title, encoding, lang, split_dir):
        self.path       = path
        self.extension  = extension
        self.postfix    = postfix
        self.book_title = book_title
        self.encoding   = encoding
        self.lang       = lang
        self.split_dir  = split_dir

    def build_tags(self, dir_short):
        tags = [self.book_title, dir_short]
        if self.split_dir:
            tags.append(dir_short.replace(os.path.sep, " ")) # Linux / Windows \\ パス区切り文字
        return " ".join(tags)

    def compile_tiddly_wiki(self):
        string_builder = []

        file_to_save = "".join(["tiddlywiki_", self.postfix, ".html"])
        file_partial = "".join(["tiddlywiki_", self.postfix, "-part.html"])

        tid_head = read_text('tidHeadMd.html', "utf-8")
        tid_foot = read_text('tidFootMd.html', "utf-8")

        src_files = glob.glob(self.path + "**/*." + self.extension, recursive=True)

        title_set = set([])
        for src_path in src_files:
            # /home/ryoji/Downloads/nadesiko_sample/sample/Chap04/04-02/ボタン２.nako
            contents = read_text(src_path, self.encoding).strip() # 行末の空白は取り除く
            basename = os.path.basename(src_path)  # ファイル名 https://note.nkmk.me/python-os-basename-dirname-split-splitext/
            title = os.path.splitext(basename)[0]  # 拡張子を取り除く
            # ボタン２
            print(title)
            unique_title = get_unique_title(title, title_set)
            # ボタン２_2
            print(unique_title)
            dir_path = os.path.dirname(src_path)
            # /home/ryoji/Downloads/nadesiko_sample/sample/Chap04/04-02/
            print(dir_path)
            # Chap04/04-02 <- /home/ryoji/Downloads/nadesiko_sample/sample/Chap04/04-02/ - /home/ryoji/Downloads/nadesiko_sample/sample/
            dir_short = dir_path[len(self.path):]
            tags = self.build_tags(dir_short)
            string_builder.append("<div title=\"")
            string_builder.append(unique_title)
            string_builder.append("\" creator=\"system\" modifier=\"system\" created=\"201001010101\" modified=\"201001010101\"")
            string_builder.append(" tags=\"")
            string_builder.append(tags)
            string_builder.append("\" changecount=\"1\">\n")
            string_builder.append("\n<pre>&lt;md&gt;\n```")
            string_builder.append(self.lang)
            string_builder.append("\n")
            string_builder.append(contents.replace("<", "&lt;").replace(">", "&gt;"))
            string_builder.append("\n```\n&lt;/md&gt;</pre>\n")
            string_builder.append("</div>\n")

        write_text(file_partial, "".join(string_builder))
        string_builder.insert(0, "<!-- book tiddlers -->\n")
        string_builder.insert(0, tid_head)
        string_builder.append("<!-- /book tiddlers -->\n")
        string_builder.append(tid_foot)
        write_text(file_to_save, "".join(string_builder))

if __name__  == '__main__':
    tsm = TiddlySrcMaker(
        book_title  ="なでしこ公式ガイドブック",
        path        ="/home/ryoji/Downloads/nadesiko_sample/sample/",
        extension   ="nako",
        postfix     ="nadeshiko",
        encoding    ="shift-jis",
        lang        = "console",
        split_dir   =True
    )
    tsm.compile_tiddly_wiki()