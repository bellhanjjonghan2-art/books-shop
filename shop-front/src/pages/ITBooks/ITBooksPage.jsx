import BookListPage from '../../components/BookListPage/BookListPage'
import { IT_BOOKS } from '../../data/itBooksData'

export default function ITBooksPage() {
  return <BookListPage categoryTitle="IT 서적" books={IT_BOOKS} />
}
