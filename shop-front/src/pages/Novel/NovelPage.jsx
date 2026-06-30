import BookListPage from '../../components/BookListPage/BookListPage'
import { NOVEL_BOOKS } from '../../data/novelData'

export default function NovelPage() {
  return <BookListPage categoryTitle="소설" books={NOVEL_BOOKS} />
}
