package main

import (
	"reflect"
	"testing"
)

func TestGlob(t *testing.T) {
	result, err := glob(".", ".java")
	if err != nil {
		t.Fatalf("failed test %#v", err)
	}
	a := []string{"src/main/java/TiddlyMaker.java"}
	if !reflect.DeepEqual(result, a) {
		t.Fatal("failed test")
	}
}

func TestFileNameWoExt(t *testing.T) {
	filePath := "/home/ryoji/Downloads/java-recipe-example/src/jp/co/shoeisha/javarecipe/chapter11/recipe305/MemoryUsageSample.java"
	result := fileNameWoExt(filePath)
	if result != "MemoryUsageSample" {
		t.Fatal("failed test")
	}
}

func TestDirTag(t *testing.T) {
	rootPath := "/home/ryoji/Downloads/java-recipe-example/src/jp/co/shoeisha/javarecipe/"
	filePath := "/home/ryoji/Downloads/java-recipe-example/src/jp/co/shoeisha/javarecipe/chapter11/recipe305/MemoryUsageSample.java"
	result := dirTag(rootPath, filePath)
	if result != "chapter11/recipe305" {
		t.Fatal("failed test")
	}
}

func TestUnique(t *testing.T) {
	// https://stackoverflow.com/questions/31064688/which-is-the-nicer-way-to-initialize-a-map-in-golang
	// Initializes a map with an entry relating the name "bob" to the number 5
	titles := map[string]interface{}{"SystemPropertiesSample": nil}
	t1 := unique("SystemPropertiesSample", titles)
	t2 := unique("SystemPropertiesSample", titles)
	if t1 != "SystemPropertiesSample_2" {
		t.Fatal("failed test")
	}
	if t2 != "SystemPropertiesSample_3" {
		t.Fatal("failed test")
	}
}
