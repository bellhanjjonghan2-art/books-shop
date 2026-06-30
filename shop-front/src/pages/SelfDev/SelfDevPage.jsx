import BookListPage from '../../components/BookListPage/BookListPage'
import { SELF_DEV_BOOKS } from '../../data/selfDevData'

export default function SelfDevPage() {
  return <BookListPage categoryTitle="자기계발서" books={SELF_DEV_BOOKS} />
}
