import { Route, Routes } from 'react-router'
import { ConsultationOfferingPage } from '@/features/offering/ConsultationOfferingPage'
import { ConsultationOfferingsPage } from '@/features/offering/ConsultationOfferingsPage'
import { NewConsultationOfferingPage } from '@/features/offering/NewConsultationOfferingPage'
import { OfferingDetailPage } from '@/features/shop/OfferingDetailPage'
import { ShopPage } from '@/features/shop/ShopPage'
import { LoginPage } from '@/features/user/LoginPage'
import { Layout } from './Layout'
import { NotFoundPage } from './NotFoundPage'
import { RequireRole } from './RequireRole'

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<ShopPage />} />
        <Route path="offerings/:id" element={<OfferingDetailPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route element={<RequireRole role="ADMIN" />}>
          <Route
            path="admin/consultations"
            element={<ConsultationOfferingsPage />}
          />
          <Route
            path="admin/consultations/new"
            element={<NewConsultationOfferingPage />}
          />
          <Route
            path="admin/consultations/:id"
            element={<ConsultationOfferingPage />}
          />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
