
# 1. front-end 측 구현 
## 1.1 목적
- 메인페이지 만들기 

## 1.2. 디자인
- @docs/resources/main-page.png  참고

## 1.3 기능
- 데이터는 서버와 api 연동
- 로그인 없어도 화면이 보일수 있도록 처리

## 1.4. 데이터
- back-end 와 연동해서 데이터를 가져온다

## 1.5 API 설명
- end-point : '/api/books'
- method : get
- 매개변수 : 없음
- response Data
```
{
   "code" : 200,
   "data" : {
     "bestTopN" : [
        {
          "Id" :"도서Id",
          "title" : "타이틀",
          "coverImage" :"이미지경로",
          "author" :"저자",
          "판매가" : "15000"
        },
     ],
      "newTopN" : [
        {
          "Id" :"도서Id",
          "title" : "타이틀",
          "coverImage" :"이미지경로",
          "author" :"저자",
          "판매가" : "15000"
        },
     ],
      "ItTopN" : [
        {
          "Id" :"도서Id",
          "title" : "타이틀",
          "coverImage" :"이미지경로",
          "author" :"저자",
          "판매가" : "15000"
        },
     ],
      "novelTopN" : [
        {
          "Id" :"도서Id",
          "title" : "타이틀",
          "coverImage" :"이미지경로",
          "author" :"저자",
          "판매가" : "15000"
        },
     ],
       "selfTopN" : [
        {
          "Id" :"도서Id",
          "title" : "타이틀",
          "coverImage" :"이미지경로",
          "author" :"저자",
          "판매가" : "15000"
        },
     ]
   }
}
```

# 2. back-end 측 구현 

## 2.1 Book API
- 책관련 클래스는 Book- 접두사를 붙여라 
- default end-point 는 api/books

## 2.2 API 기능 정의
- 베스트 셀러, 신간, 각 도서의 top5 데이터를 제공
- 현재 Top5 는 출간일 기준으로 내림차순하여 자른다.
- 베스트 셀러, 신간도 마찬가지로  출판일 기준으로 내림차순하여 5개씩 추출.
- 만약 출간일이 같다면 이름순으로 결정 
- 베스트셀러는 테이블에서 best_yn 이 'Y' 인것
- 신간은 테이블에서 new_yn 이 'Y' 인것

## 2.3 API 스펙
- front-end 측 구현에서 정의한 response 데이터를 참고하여 데이터 구성 
- 로그인 없이 접근 가능 

## 2.4 테이블 구조
- @scripts/create_table.sql 파일 참고

# 3. 개발 유의 사항
- 문서에 없는 내용 개발 금지
- 관련없는 기존 코드 수정 금지
- Enum 을 사용할 때 Enum 객체로 비교 말고 String 값으로 DB비교
- 테이블에 Enum 객체 없을 경우, select 시 Enum 객체로 비교 금지
