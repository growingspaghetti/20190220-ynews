package main

import (
	"bytes"
	"fmt"
	"io"
	"io/ioutil"
	"os"
	"path/filepath"
	"strconv"
	"strings"

	"golang.org/x/text/encoding/japanese"
	"golang.org/x/text/transform"
)

type tiddlyMaker struct {
	path, extension, postfix, btitle, charset, lang string
	splitDir                                        bool
}

// https://gobyexample.com/reading-files
func check(e error) {
	if e != nil {
		panic(e)
	}
}

// https://ja.stackoverflow.com/questions/6120/go%E3%81%A7byte%E3%82%92shift-jis%E3%81%AE%E6%96%87%E5%AD%97%E5%88%97%E3%81%AB%E5%A4%89%E6%8F%9B%E3%81%99%E3%82%8B
// go get "golang.org/x/text/transform"
func transformEncoding(rawReader io.Reader, trans transform.Transformer) (string, error) {
	ret, err := ioutil.ReadAll(transform.NewReader(rawReader, trans))
	if err == nil {
		return string(ret), nil
	}
	return "", err
}

// FromShiftJIS converts a string encoding from ShiftJIS to UTF-8
// go get "golang.org/x/text/encoding/japanese"
func FromShiftJIS(str string) (string, error) {
	return transformEncoding(strings.NewReader(str), japanese.ShiftJIS.NewDecoder())
}

// read a file as text
func readContents(path, charset string) string {
	contents, _ := ioutil.ReadFile(path)
	switch {
	case charset == "shift-jis":
		dec, _ := FromShiftJIS(string(contents))
		return dec
	case charset == "utf-8":
		return string(contents)
	default:
		panic("unknown charset. allowed:[shift-jis, utf-8]")
	}
}

// go interface language design http://jxck.hatenablog.com/entry/20130325/1364251563
// https://yourbasic.org/golang/implement-set/
func unique(s string, set map[string]interface{}) string {
	if _, ok := set[s]; !ok {
		set[s] = nil
		return s
	}
	for i := 2; ; i++ {
		unique := s + "_" + strconv.Itoa(i)
		if _, ok := set[unique]; !ok {
			set[unique] = nil
			return unique
		}
	}
}

func fileNameWoExt(path string) string {
	basename := filepath.Base(path)
	return strings.TrimSuffix(basename, filepath.Ext(basename))
}

// https://stackoverflow.com/questions/26809484/how-to-use-double-star-glob-in-go
func glob(dir string, ext string) ([]string, error) {
	files := []string{}
	err := filepath.Walk(dir, func(path string, f os.FileInfo, err error) error {
		if filepath.Ext(path) == ext {
			files = append(files, path)
		}
		return nil
	})
	return files, err
}

// chapter11/recipe305
func dirTag(rootPath, filePath string) string {
	dir, _ := filepath.Split(filePath)
	rel, _ := filepath.Rel(rootPath, dir)
	return rel
}

func (tm *tiddlyMaker) buildTag(dir string) string {
	tags := []string{dir}
	if tm.splitDir {
		tags = append(tags, strings.Replace(dir, string(os.PathSeparator), " ", -1))
	}
	return strings.Join(tags, " ")
}

func (tm *tiddlyMaker) compileTiddlyWiki() {
	titles := make(map[string]interface{}, 0)
	var sb bytes.Buffer
	pz, _ := glob(tm.path, "."+tm.extension)
	for _, ps := range pz {
		title := fileNameWoExt(ps)
		title = unique(title, titles)
		contents := readContents(ps, tm.charset)
		contents = strings.Replace(contents, "<", "&lt;", -1)
		contents = strings.Replace(contents, ">", "&gt;", -1)
		contents = strings.TrimSpace(contents)
		tags := tm.buildTag(dirTag(tm.path, ps))
		sb.WriteString("<div title=\"")
		sb.WriteString(title)
		sb.WriteString("\" creator=\"system\" modifier=\"system\" created=\"201001010101\" modified=\"201001010101\"")
		sb.WriteString(" tags=\"")
		sb.WriteString(tags)
		sb.WriteString("\" changecount=\"1\">\n")
		sb.WriteString("\n<pre>&lt;md&gt;\n```")
		sb.WriteString(tm.lang)
		sb.WriteString("\n")
		sb.WriteString(contents)
		sb.WriteString("\n```\n&lt;/md&gt;</pre>\n")
		sb.WriteString("</div>\n")
	}
	partF := fmt.Sprintf("%s%s%s", "tiddlywiki_", tm.postfix, "-part.html")
	ioutil.WriteFile(partF, sb.Bytes(), 0600)

	toSave := fmt.Sprintf("%s%s%s", "tiddlywiki_", tm.postfix, ".html")
	tidHead, _ := ioutil.ReadFile("tidHeadMd.html")
	tidFoot, _ := ioutil.ReadFile("tidFootMd.html")

	ioutil.WriteFile(toSave, bytes.Join([][]byte{
		tidHead,
		[]byte("<!-- book tiddlers -->\n"),
		sb.Bytes(),
		[]byte("<!-- /book tiddlers -->\n"),
		tidFoot,
	}, []byte{}), 0600)
}

func main() {
	tm := &tiddlyMaker{
		path:      "/home/ryoji/Downloads/java-recipe-example/src/jp/co/shoeisha/javarecipe/",
		extension: "java",
		postfix:   "java-recipe",
		btitle:    "Java逆引きレシピ",
		charset:   "utf-8",
		lang:      "java",
		splitDir:  true,
	}
	tm.compileTiddlyWiki()
}
