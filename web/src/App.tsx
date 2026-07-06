import { Routes, Route } from 'react-router-dom'
import { Sidebar } from './components/Sidebar'
import { TopBar } from './components/TopBar'
import { TopBarProvider } from './context/TopBarContext'
import { Dashboard } from './pages/Dashboard'
import { Library } from './pages/Library'
import { Templates } from './pages/Templates'
import { Paths } from './pages/Paths'
import { FullRun } from './pages/runs/FullRun'

function App() {
  return (
    <TopBarProvider>
      <div className="min-h-screen">
        <Sidebar />
        <div className="ml-64">
          <TopBar />
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/new-run/full" element={<FullRun />} />
            <Route path="/library" element={<Library />} />
            <Route path="/templates" element={<Templates />} />
            <Route path="/paths" element={<Paths />} />
          </Routes>
        </div>
      </div>
    </TopBarProvider>
  )
}

export default App
