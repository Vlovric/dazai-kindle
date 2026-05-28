## Plan: Java Migration of KindleParser

Java rewrite of the Kindle clippings pipeline, delegating parsing to the Fyodor Ruby subprocess while supporting custom Mustache output templates and using Maven + args4j.

# Refactor ground up

# 1 Args
- book path (.azw3 ili .epub)
- clippings path (.txt)
- template path (.mustache)
- debug mode
# 2 AZW3 conversion
- koristi `ebook-convert`
- converta iz AZW3 u epub ako je potrebno, vraca path
- ako je vec epub ili epub postoji onda samo path
# 3 EPUB extraction i sve osim TOC
- unzippa epub u temp dir (<mark class="hltr-blue">debug permanent dir</mark>)
- U `META-INF/container.xml` dobiva <mark class="hltr-red">full-path</mark> za <mark class="hltr-purple">content.opf</mark>:
```xml
<?xml version="1.0"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
   <rootfiles>
      <rootfile full-path="content.opf" media-type="application/oebps-package+xml"/>
      
   </rootfiles>
</container>
    
```

- iz <mark class="hltr-purple">content.opf</mark> dobiva <mark class="hltr-red">dc:title</mark> i <mark class="hltr-red">dc:creator</mark>
- **content.opf** struktura:
```xml
<package>
	<metadata>
		<dc:...>
		...
	</metadata>
	<manifest>
		<item id="id102" href="text/part0000.html" media type="application/xhtml+xml"/>
		<item id="id...", href, media-type/>
		....
		<item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
	</manifest>
	<spine toc="ncx">
		<itemref idref/>
		...
	</spine toc="ncx>
	<guide>
	</guide>
</package>
```
- <mark class="hltr-red">item</mark> je dio knjige
- <mark class="hltr-red">itemref</mark> u spine je poredak kak se cita

- item id="ncx" je EPUB2 TOC
- nav.xhtml bi bio EPUB3 TOC

- spremam svaki <mark class="hltr-red">item ID sa pravim file pathom</mark>
- spremam <mark class="hltr-red">poredak spine itemref-ova</mark> kako bi znao poredak
# 4 TOC parsing
## .ncx (EPUB2)
```xml
<ncx>
	<head>
	</head>
	
	<docTitle>
	</docTitle>
	
	<navMap>
		<navPoint> (samo H1)
			<navLabel>
				<text>Naziv H1</text>
			</navLabel>
			<content src="text/part0003.html"/>
		</navPoint>
		
		<navPoint> (H1 sa H2 u sebi)
			<navLabel>
				<text>Naziv H1</text>
			</navLabel>
			<content src="text/part0005.html"/>
			
			<navPoint>
				<navLabel>
					<text>Naziv H2</text>
				</navLabel>
				<content src="text/part0005.html#intro1"/>
			</navPoint
			
		</navPoint>
		
```
- <mark class="hltr-red">navPoint</mark> je heading, gledam nesting za razine
- <mark class="hltr-red">content src</mark> je path, ima # za sekciju
- - -
## .xhtml (EPUB3)
- If `.xhtml`/`.html`: traverse `<nav>` → `<ol>` → `<li>` → `<a>` → extract `href` (file + anchor), level
- - -
## Output
- <mark class="hltr-blue">outputat u debug folder</mark>
```json
[
  TocEntry(title="About the authors", file="text/part0003.html", anchor=null, level=1),
  TocEntry(title="Introduction", file="text/part0005.html", anchor=null, level=1),
  TocEntry(title="How to use this guide", file="text/part0005.html", anchor="intro1", level=2)
]
```
- anchor je sekcija sa #, level je heading level prema nestingu
# 5 TOC lokacija
## File offset (buildFileOffsets())
- idem po svakom fileu iz spinea
- za svaki file uzmem sve charactere
- tom fileu izracunam offset prema koliko je charactera prethodilo tom fileu
```
npr
- File 1: 5000 chars → offset=0
- File 2: 3000 chars → offset=5000
- File 3: 2000 chars → offset=8000
```
## Anchor offset (anchorOffset())
- za svaki file koji ima anchor tj nested headinge
- uzme offset do tog filea i gleda koliko charactera do tog elementa tj sekcije
- zbroji to i dobije poziciju headinga nize razine
```
npr
- File offset for `part0005.html` = 5000
- Text before `<h1 id="intro1">` = 1200 chars
- Total = 6200
```
## Racunanje kindle lokacije
- kindle lokacija je `(offset / 128) + 1`
- 128 bajta je 1 kindle location
- + 1 jer lokacije pocinju na 1, a ne 0
```
npr
- 0-127 bytes → location 1
- 128-255 bytes → location 2
- 256-383 bytes → location 3
```
## Output
- `lista (tocEntry, charOffset, location)`
- <mark class="hltr-blue">outputat u debug folder</mark>
# 6 Fyodor
- <mark class="hltr-red">template.erb</mark> se sprema u runtimeu ako ne postoji
- <mark class="hltr-red">fyodor.toml </mark>mora postojat sa konkretnim: <mark class="hltr-yellow">File name format???</mark>
```
[output]
filename = "????.json"
```
- izvrsava se fyodor process sa clippings i output dirom
- cita se JSON file:
<mark class="hltr-yellow">koje sve atribute fyodor daje?</mark>
<mark class="hltr-yellow">radi li to_json?</mark>
<mark class="hltr-yellow">Koji sve tipovi clippinga postoje?</mark>
```json
<%#
  Each line is a valid JSON object with these fields:
    book_title  – full title string as Fyodor sees it
    author      – author string (may be empty)
    type        – "highlight", "note", "bookmark", or "clip"
    loc         – integer Kindle location, or null if not parsed
    page        – integer page number, or null if not parsed
    date        – raw date string from the clippings file
    text        – the highlight or note content
-%>
<% require 'json' -%>
<% for entry in regular_entries -%>
{"book_title":<%= @book.title.to_json %>,"author":<%= @book.author.to_json %>,"type":<%= entry.type.to_s.to_json %>,"loc":<%= entry.loc.nil? ? "null" : entry.loc.to_json %>,"page":<%= entry.page.nil? ? "null" : entry.page.to_json %>,"date":<%= entry.desc.to_json %>,"text":<%= entry.text.to_json %>}
<% end -%>
<% for entry in bookmarks -%>
{"book_title":<%= @book.title.to_json %>,"author":<%= @book.author.to_json %>,"type":"bookmark","loc":<%= entry.loc.nil? ? "null" : entry.loc.to_json %>,"page":<%= entry.page.nil? ? "null" : entry.page.to_json %>,"date":<%= entry.desc.to_json %>,"text":""}
<% end -%>
```
- <mark class="hltr-yellow">ucitat parsean .json prema filenameu</mark>!!
- <mark class="hltr-blue">U debug spremit ovaj json output da je readable</mark>
- **entry.loc** daje `"loc": "1847-1852"` ili `"loc": "1847"`
## Output
- Svaka linija je `Clipping(book_title, author, type, loc, page, date, text)`
- za loc se uzima samo prvi broj ako je dan raspon iz fyodora
# 7 Grupiranje
- imam listu `(tocEntry, charOffset, location)`
- imam listu `Clipping(book_title, author, type, loc, page, date, text)`
- Stvorit mapu `HeadingGroup(heading, clippings)` <mark class="hltr-yellow">gdje je clippings lista ig?</mark>
- <mark class="hltr-yellow">Ne droppat empty groups</mark>
- <mark class="hltr-blue">exportat objekte u readable format</mark>
# 8 Template renderanje
- <mark class="hltr-yellow">koje atribute mogu koristit u .mustache templateu?</mark>
- <mark class="hltr-yellow">Kak uopce .mustache funkcionira??</mark>
<mark class="hltr-yellow">- Koristit nesto drugo ako mi treba vise logike?</mark>

Template treba ici:
```
# Naslov H1
>[!kindle-quote] Highlight
>Ako note slijedi highlight onda ide ovdje
## Naslov H2
>[!kindle-quote Highlight]
> prazan body ako je sljedece Highlight
...
>... 
```
# 8 Debug flag sto sve treba
- unzippani epub smjestit u permanent debug directory u projektu
- output TOC parsinga i kao objekt to string i kao markdown da mogu prekontrolirat lako
- outputat listu `lista (tocEntry, charOffset, location)` TOC lokacija kao objekt to string
- spremit fyodor json kao readable json da provjerim
- outputat grupirane clippinge sa headingsima kao objekt to string