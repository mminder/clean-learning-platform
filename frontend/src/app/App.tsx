import { Route, Routes } from 'react-router'
import { OfferingDetailPage } from '@/features/shop/OfferingDetailPage'
import { ShopPage } from '@/features/shop/ShopPage'
import { LoginPage } from '@/features/user/LoginPage'
import { Layout } from './Layout'
import { NotFoundPage } from './NotFoundPage'

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<ShopPage />} />
        <Route path="offerings/:id" element={<OfferingDetailPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
