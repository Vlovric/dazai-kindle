import { Routes, Route } from 'react-router-dom'
import { Sidebar } from './components/Sidebar'
import { TopBar } from './components/TopBar'
import { TopBarProvider } from './context/TopBarContext'
import { Dashboard } from './pages/Dashboard'
import { Library } from './pages/Library'
import { BookDetail } from './pages/BookDetail'
import { ClippingsFile } from './pages/ClippingsFile'
import { Templates } from './pages/Templates'
import { Paths } from './pages/Paths'
import { FullRun } from './pages/runs/FullRun'
import { CalibrationRun } from './pages/runs/CalibrationRun'
import { HeadingsRun } from './pages/runs/HeadingsRun'

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
            <Route path="/new-run/calibration" element={<CalibrationRun />} />
            <Route path="/new-run/headings" element={<HeadingsRun />} />
            <Route path="/library" element={<Library />} />
            <Route path="/library/:name" element={<BookDetail />} />
            <Route path="/clippings" element={<ClippingsFile />} />
            <Route path="/templates" element={<Templates />} />
            <Route path="/paths" element={<Paths />} />
          </Routes>
        </div>
      </div>
    </TopBarProvider>
  )
}

export default App
