import { Navigate, type RouteObject } from 'react-router'
import { GuestRoute } from '@/features/auth/GuestRoute'
import ForgotPasswordPage from '@/features/auth/pages/ForgotPasswordPage'
import LoginPage from '@/features/auth/pages/LoginPage'
import RegisterPage from '@/features/auth/pages/RegisterPage'
import { ProtectedRoute } from '@/features/auth/ProtectedRoute'
import NewOrderPage from '@/features/orders/pages/NewOrderPage'
import OrderDetailPage from '@/features/orders/pages/OrderDetailPage'
import OrdersListPage from '@/features/orders/pages/OrdersListPage'
import { AppLayout } from '@/app/layout/AppLayout'
import NotFoundPage from './NotFoundPage'

// Exported as plain route objects so tests can mount them in a memory router.
export const routes: RouteObject[] = [
  { path: '/', element: <Navigate to="/orders" replace /> },
  {
    element: <GuestRoute />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/forgot-password', element: <ForgotPasswordPage /> },
    ],
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: '/orders', element: <OrdersListPage /> },
          { path: '/orders/new', element: <NewOrderPage /> },
          { path: '/orders/:id', element: <OrderDetailPage /> },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
]
