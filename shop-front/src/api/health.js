import axios from 'axios'

export async function fetchHealth() {
  const { data } = await axios.get('/api/health')
  return data
}
