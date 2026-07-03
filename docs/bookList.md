
# 1. 카데고리별 도서 리스트 API 만들기
- 메뉴(카테고리)를 선택하면 해당 메뉴에 맞는 도서리스트를 출력
- category 테이블의 types가 IT(IT서적), NOVEL(소설), SELF(자기개발서)
- BookListPage.jsx(메뉴별 도서리스트) 화면에 대응하는 API 
- 리스트는 페이징 처리


# 2. API 서버 정보
- end-point : /api/books/{types}
- types PathVariable 처리
- 매개변수 : page, size, orderType(정렬 값)
- 데이터 값은 IT, NOVEL, SELF
- 응답 데이터
```
{ 
    "code" : 200
    "data" : [
        {
            "bookId" : "책 ID",
            "title" : "제목",
            "subtitle" : "부제목",
            "author" : "저자",
            "publisher" : "출판사",    
            "publishedDate" : "출판일",
            "totalReviewCnt" : "전체 리뷰수",
            "reviewRating" : "리뷰점수 평균값",
            "listPrice" : "정가",
            "salePrice" : "판매가",
            "coverImage" : "이미지"
        },
    ]
}
```
# 3. 데이터 출력 유의 사항
- 기본 정렬은 출판일이 최신인 것부터 출력
- 출판일이 같다면 제목 순으로 출력
- 낮은가격순, 높은가격순, 리뷰 수가 많은 순서
- 가격은 판매가(salePrice) 기준으로 정렬
- orderType은 최신순(new), 높은가격순(high), 낮은가격순(lower), 리뷰많은순(reviewCnt)