import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ChatPage from './pages/ChatPage';
import ProtectedRoute from './components/common/ProtectedRoute';
import Toast from './components/common/Toast';
import NewChatModal from './components/modals/NewChatModal';
import NewGroupModal from './components/modals/NewGroupModal';
import ProfileModal from './components/modals/ProfileModal';
import GroupInfoModal from './components/modals/GroupInfoModal';
import { useUiStore } from './stores/uiStore';

function ModalManager() {
  const activeModal = useUiStore((s) => s.activeModal);

  switch (activeModal) {
    case 'newChat': return <NewChatModal />;
    case 'newGroup': return <NewGroupModal />;
    case 'profile': return <ProfileModal />;
    case 'groupInfo': return <GroupInfoModal />;
    default: return null;
  }
}

export default function App() {
  return (
    <BrowserRouter>
      <Toast />
      <ModalManager />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/chat" element={<ProtectedRoute><ChatPage /></ProtectedRoute>} />
        <Route path="*" element={<Navigate to="/chat" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
