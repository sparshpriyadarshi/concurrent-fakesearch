/*
This example was presented by Rob Pike for the Go concurrency patterns talk at:
All talks: https://go.dev/talks/2012/
This talk: https://go.dev/talks/2012/concurrency.slide#1
Video    : https://www.youtube.com/watch?v=f6kdp27TYZs
*/
package main

import (
	"fmt"
	"math/rand"
	"os"
	"time"
)

var (
	Web   = fakeSearch("web")
	Image = fakeSearch("image")
	Video = fakeSearch("video")

	//for V4
	Web1   = fakeSearch("web")
	Image1 = fakeSearch("image")
	Video1 = fakeSearch("video")
	Web2   = fakeSearch("web")
	Image2 = fakeSearch("image")
	Video2 = fakeSearch("video")
)

type Result string
type Search func(query string) Result

func fakeSearch(kind string) Search {
	return func(query string) Result {
		prng := rand.New(rand.NewSource(time.Now().UnixNano()))
		time.Sleep(time.Duration(prng.Intn(100)) * time.Millisecond)
		return Result(fmt.Sprintf("%s-result-for:%q", kind, query))
	}
}
func First(query string, replicas ...Search) Result {
	c := make(chan Result)
	searchReplica := func(i int) { c <- replicas[i](query) }
	for i := range replicas {
		go searchReplica(i)
	}
	return <-c
}

func Googl1(query string) (results []Result) { //v1
	results = append(results, Web(query))
	results = append(results, Image(query))
	results = append(results, Video(query))
	return
}

func Googl2(query string) (results []Result) { //v2
	c := make(chan Result)
	go func() { c <- Web(query) }()
	go func() { c <- Image(query) }()
	go func() { c <- Video(query) }()

	for i := 0; i < 3; i++ {
		result := <-c
		results = append(results, result)
	}
	return
}

func Googl3(query string) (results []Result) { //v3
	c := make(chan Result)
	go func() { c <- Web(query) }()
	go func() { c <- Image(query) }()
	go func() { c <- Video(query) }()

	timeout := time.After(80 * time.Millisecond)
	for i := 0; i < 3; i++ {
		select {
		case result := <-c:
			results = append(results, result)
		case <-timeout:
			//fmt.Println("timed-out")
			results = append(results, "timed-out")
			return
		}
	}
	return
}

func Googl4(query string) (results []Result) { //v4
	c := make(chan Result)
	go func() { c <- First(query, Web1, Web2) }()
	go func() { c <- First(query, Image1, Image2) }()
	go func() { c <- First(query, Video1, Video2) }()

	timeout := time.After(80 * time.Millisecond)
	for i := 0; i < 3; i++ {
		select {
		case result := <-c:
			results = append(results, result)
		case <-timeout:
			//fmt.Println("timed-out")
			results = append(results, "timed-out")
			return
		}
	}
	return
}
func RunAll() {
	versions := 4
	iterations := 100
	for v := 1; v <= versions; v++ {
		for i := 0; i < iterations; i++ {
			var results []Result
			searchquery := "Go is awesome"
			start := time.Now()
			switch v {
			case 1:
				results = Googl1(searchquery)
			case 2:
				results = Googl2(searchquery)
			case 3:
				results = Googl3(searchquery)
			case 4:
				results = Googl4(searchquery)
			default:
				fmt.Fprintln(os.Stderr, "Error, check version for search call")
			}
			elapsed := time.Since(start)
			fmt.Printf("Go results, V%v, %s, %v ms\n", v, results, elapsed.Milliseconds()) //elapsed is microseconds sometimes TODO FIXME:

		}

	}
	// for i := 0; i < iterations; i++ {
	// 	start := time.Now()
	// 	results := Googl1("Go is awesome")
	// 	elapsed := time.Since(start)
	// 	fmt.Printf("Go results, V1, %s, %s\n", results, elapsed)

	// }
	// for i := 0; i < iterations; i++ {
	// 	start := time.Now()
	// 	results := Googl2("Go is awesome")
	// 	elapsed := time.Since(start)
	// 	fmt.Printf("Go results, V2, %s, %s\n", results, elapsed)

	// }
	// for i := 0; i < iterations; i++ {
	// 	start := time.Now()
	// 	results := Googl3("Go is awesome")
	// 	elapsed := time.Since(start)
	// 	fmt.Printf("Go results, V3, %s, %s\n", results, elapsed)

	// }
	// for i := 0; i < iterations; i++ {
	// 	start := time.Now()
	// 	results := Googl4("Go is awesome")
	// 	elapsed := time.Since(start)
	// 	fmt.Printf("Go results, V4, %s, %s\n", results, elapsed)

	// }

}
func main() {
	RunAll()
}
