import { Route, Routes } from 'react-router'
import { ShopPage } from '@/features/shop/ShopPage'
import { Layout } from './Layout'
import { NotFoundPage } from './NotFoundPage'

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<ShopPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
