
# 1. 도서 상세페이지 만들기
- 어떤 페이지에서든 도서 제목을 클릭하면 상세화면으로 이동
- 상세화면은 제외

# 2. front-end 구현

## 2.1 디자인
- @docs/resources/book-detail-page.png 이미지 참고하여 만들것

## 2.2 기능
- 장바구니에 담기와 바로구매 버튼에 대한 기능이 필요
- 리뷰목록 리스트를 서버로부터 가져온다
- 로그인 없이 화면이 보일 수 있다
- 장바구니와 바로구매는 회원이 사용하는 기능이라서 비로그인시에는
  '로그인이 필요한 기능입니다.'라고 출력

## 2.3 데이터 
- 데이터는 back-end 서버와 연동하여 가져온다
- rest api를 사용

## 2.4 API 설명
- end-point : '/api/books/{bookID}'
- method : Get
- 매개변수 : parameter와 pathVariable
- param : page(0부터 시작), size(크기, 한번에 보여줄 개수)
- response Data

```
{
    "code" : 200,
    "data" : {
        "bookID" : "id",
        "title"  : "타이틀",
        "coverImage" : "이미지경로",
        "author"  : "저자",
        "listPrice"  : "원가",
        "salePrice"  : "세일가(최종판매가)",
        "publisher"  : "출판사",
        "stocks"   :  "보유 수",
        "publishedData"  : "출판일",
        "description"  : "책 설명",
        "reviewList"  : [....]
    }
}
```

# 3. back-end 구현
- 선택된 도서의 상세정보를 API로 제공

## 3.1 API 스펙
- front 스펙 2.4  API 설명 참고

# 4. 주의사항
- 로그인 없이 진입 가능
- 장바구니, 바로구매는 로그인 유저만 가능
- 비 로그인 시 두 버튼을 누르면 경고창 팝업

    