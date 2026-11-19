import { useState } from 'react'
import './App.css'

const accounts = {
  Administrator: { username: 'Admin', password: 'ADMINVVIT' },
  HOD: { username: 'HOD', password: 'HODVVIT' },
  Faculty: { username: 'Faculty1', password: 'VVIT' },
}

const publicRoles = ['Administrator', 'HOD', 'Faculty', 'Student']

const portals = {
  Administrator: [
    ['Students', 'View and maintain student attendance records'],
  ],
  HOD: [
    ['Attendance', 'Update institutional student attendance'],
  ],
  Faculty: [
    ['Attendance', 'Set subject-wise attended totals for each student'],
  ],
  Student: [
    ['My Subjects', 'Subject-wise attendance summary'],
  ],
}

function App() {
  const [view, setView] = useState('home')
  const [activeRole, setActiveRole] = useState(null)
  const [activeSection, setActiveSection] = useState(null)
  const [session, setSession] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [apiResult, setApiResult] = useState(null)
  const [attendance, setAttendance] = useState(null)
  const [studentId, setStudentId] = useState('')
  const [studentPassword, setStudentPassword] = useState('')
  const [signupMode, setSignupMode] = useState(false)
  const [portalCredentials, setPortalCredentials] = useState(null)
  const [attendanceList, setAttendanceList] = useState([])
  const [selectedStudent, setSelectedStudent] = useState('')
  const [selectedSubject, setSelectedSubject] = useState('DSA')
  const [conductedSessions, setConductedSessions] = useState(50)
  const [attendedSessions, setAttendedSessions] = useState(30)
  const [roleLoginFor, setRoleLoginFor] = useState(null)
  const [roleLoginId, setRoleLoginId] = useState('')
  const [roleLoginPassword, setRoleLoginPassword] = useState('')

  function beginRoleLogin(role) {
    setRoleLoginFor(role)
    setRoleLoginId('')
    setRoleLoginPassword('')
    setError('')
  }

  async function login(role) {
    if (role === 'Student') {
      return loginStudent()
    }
    const enteredId = roleLoginId.trim()
    const enteredPassword = roleLoginPassword
    const expected = accounts[role]
    if (enteredId !== expected.username || enteredPassword !== expected.password) {
      setError(`Invalid ID or Password`)
      return
    }
    setLoading(true)
    setError('')
    try {
      const credentials = btoa(`${enteredId}:${enteredPassword}`)
      const response = await fetch('http://localhost:8080/api/me', {
        headers: { Authorization: `Basic ${credentials}` },
      })
      if (!response.ok) throw new Error('Invalid portal credentials')
      setSession(await response.json())
      setActiveRole(role)
      setActiveSection(portals[role][0][0])
      setPortalCredentials(credentials)
      if (role !== 'Student') {
        await loadAttendanceList(credentials, role)
      }
      setView('portal')
    } catch (loginError) {
      setError(loginError.message)
    } finally {
      setLoading(false)
    }
  }

  async function loginStudent() {
    setLoading(true)
    setError('')
    try {
      const credentials = btoa(`${studentId}:${studentPassword}`)
      const response = await fetch('http://localhost:8080/api/me', {
        headers: { Authorization: `Basic ${credentials}` },
      })
      if (!response.ok) throw new Error('Invalid ID or Password')
      setSession(await response.json())
      setActiveRole('Student')
      setActiveSection(portals.Student[0][0])
      setPortalCredentials(credentials)
      setView('portal')
      await loadStudentAttendance(credentials)
    } catch (loginError) {
      setError(loginError.message)
    } finally {
      setLoading(false)
    }
  }

  async function signupStudent() {
    setLoading(true)
    setError('')
    try {
      const response = await fetch('http://localhost:8080/api/public/student/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ studentId, password: studentPassword }),
      })
      const result = await response.json()
      if (!response.ok) throw new Error(result.message || 'Student signup failed')
      setApiResult({ title: 'Signup successful', permissions: result.message })
      setSignupMode(false)
    } catch (signupError) {
      setError(signupError.message)
    } finally {
      setLoading(false)
    }
  }

  function logout() {
    setSession(null)
    setActiveRole(null)
    setActiveSection(null)
    setApiResult(null)
    setView('home')
    setAttendance(null)
    setPortalCredentials(null)
    setStudentId('')
    setStudentPassword('')
  }

  async function loadStudentAttendance(credentials) {
    const response = await fetch('http://localhost:8080/api/student/attendance', {
      headers: { Authorization: `Basic ${credentials}` },
    })
    if (response.ok) setAttendance(await response.json())
  }

  async function saveStudentAttendance() {
    if (!selectedStudent) {
      setError('Select a student')
      return
    }
    if (attendedSessions > conductedSessions) {
      setError('Attended sessions cannot exceed conducted sessions')
      return
    }
    if (conductedSessions < 0 || attendedSessions < 0) {
      setError('Session counts cannot be negative')
      return
    }
    const endpointByRole = {
      Faculty: '/api/faculty/students/attendance',
      HOD: '/api/hod/students/attendance',
      Administrator: '/api/administrator/students/attendance',
    }
    try {
      const response = await fetch(`http://localhost:8080${endpointByRole[activeRole]}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Basic ${portalCredentials}`,
        },
        body: JSON.stringify({
          studentId: selectedStudent,
          subject: selectedSubject,
          conductedSessions,
          attendedSessions,
        }),
      })
      const result = await response.json()
      if (!response.ok) throw new Error(result.message || 'Attendance update failed')
      setApiResult({ title: 'Attendance updated', permissions: result.message })
      await loadAttendanceList(portalCredentials, activeRole)
    } catch (saveError) {
      setError(saveError.message)
    }
  }

  async function loadAttendanceList(credentials, roleOverride) {
    const endpointByRole = {
      Faculty: '/api/faculty/students/attendance',
      HOD: '/api/hod/students/attendance',
      Administrator: '/api/administrator/students/attendance',
    }
    const role = roleOverride || activeRole
    const response = await fetch(`http://localhost:8080${endpointByRole[role]}`, {
      headers: { Authorization: `Basic ${credentials}` },
    })
    if (response.ok) {
      const rows = await response.json()
      setAttendanceList(rows)
    }
  }


  function selectStudentForEditing(studentId) {
    setSelectedStudent(studentId)
    const student = attendanceList.find((row) => row.studentId === studentId)
    const subject = student?.subjects?.find((row) => row.subject === selectedSubject)
    if (subject) {
      setConductedSessions(subject.conducted)
      setAttendedSessions(subject.attended)
    }
  }

  function selectSubjectForEditing(subject) {
    setSelectedSubject(subject)
    const student = attendanceList.find((row) => row.studentId === selectedStudent)
    const record = student?.subjects?.find((row) => row.subject === subject)
    if (record) {
      setConductedSessions(record.conducted)
      setAttendedSessions(record.attended)
    }
  }

  async function openSection(title) {
    setActiveSection(title)
    setError('')
    const endpoints = {
      Administrator: '/api/administrator/dashboard',
      HOD: '/api/hod/dashboard',
      Faculty: '/api/faculty/dashboard',
      Student: '/api/student/dashboard',
    }
    try {
      let credentials = portalCredentials
      if (!credentials && activeRole !== 'Student') {
        const account = accounts[activeRole]
        credentials = btoa(`${account.username}:${account.password}`)
      }
      const response = await fetch(`http://localhost:8080${endpoints[activeRole]}`, {
        headers: { Authorization: `Basic ${credentials}` },
      })
      if (!response.ok) throw new Error(`Access denied for ${activeRole}`)
      setApiResult(await response.json())
      if (activeRole !== 'Student') {
        await loadAttendanceList(credentials)
      } else {
        await loadStudentAttendance(credentials)
      }
    } catch (sectionError) {
      setError(sectionError.message)
    }
  }

  function renderActiveSection() {
    if (activeRole === 'Student') {
      return (
        <section className="working-section">
          <h2>{activeSection}</h2>
          <table className="data-table"><thead><tr><th>Subject</th><th>Conducted</th><th>Attended</th><th>Percentage</th></tr></thead><tbody>
            {(attendance?.subjects || []).map((record) => (
              <tr key={record.subject}><td>{record.subject}</td><td>{record.conducted}</td><td>{record.attended}</td><td>{record.percentage}%</td></tr>
            ))}
          </tbody></table>
        </section>
      )
    }

    if (activeRole === 'Faculty') {
      return (
        <section className="working-section">
          <h2>{activeSection}</h2>
          {activeSection === 'Attendance' ? (
            <form className="working-form attendance-editor" onSubmit={(event) => { event.preventDefault(); saveStudentAttendance() }}>
              <label>Student<select value={selectedStudent} onChange={(event) => selectStudentForEditing(event.target.value)}><option value="">Select student</option>{attendanceList.map((row) => <option key={row.studentId} value={row.studentId}>{row.studentId}</option>)}</select></label>
              <label>Subject<select value={selectedSubject} onChange={(event) => selectSubjectForEditing(event.target.value)}><option>DSA</option><option>DBMS</option><option>OS</option><option>Python</option><option>GenAI</option></select></label>
              <label>Conducted<input type="number" value={50} disabled /></label>
              <label>Attended<input type="number" min="0" max="50" value={attendedSessions} onChange={(event) => setAttendedSessions(Number(event.target.value))} /></label>
              <button type="submit">Save Attended Count</button>
            </form>
          ) : null}
        </section>
      )
    }

    if (activeRole === 'Administrator') {
      return (
        <section className="working-section">
          <h2>{activeSection}</h2>
          <form className="working-form attendance-editor" onSubmit={(event) => { event.preventDefault(); saveStudentAttendance() }}>
            <label>Student<select value={selectedStudent} onChange={(event) => setSelectedStudent(event.target.value)}><option value="">Select student</option>{attendanceList.map((row) => <option key={row.studentId} value={row.studentId}>{row.studentId}</option>)}</select></label>
            <label>Subject<select value={selectedSubject} onChange={(event) => setSelectedSubject(event.target.value)}><option>DSA</option><option>DBMS</option><option>OS</option><option>Python</option><option>GenAI</option></select></label>
            <label>Conducted<input type="number" value={50} disabled /></label>
            <label>Attended<input type="number" min="0" max="50" value={attendedSessions} onChange={(event) => setAttendedSessions(Number(event.target.value))} /></label>
            <button type="submit">Save Attended Count</button>
          </form>
        </section>
      )
    }

    return (
      <section className="working-section">
        <h2>{activeSection}</h2>
        <form className="working-form attendance-editor" onSubmit={(event) => { event.preventDefault(); saveStudentAttendance() }}>
          <label>Student<select value={selectedStudent} onChange={(event) => setSelectedStudent(event.target.value)}><option value="">Select student</option>{attendanceList.map((row) => <option key={row.studentId} value={row.studentId}>{row.studentId}</option>)}</select></label>
          <label>Subject<select value={selectedSubject} onChange={(event) => setSelectedSubject(event.target.value)}><option>DSA</option><option>DBMS</option><option>OS</option><option>Python</option><option>GenAI</option></select></label>
          <label>Conducted<input type="number" value={50} disabled /></label>
          <label>Attended<input type="number" min="0" max="50" value={attendedSessions} onChange={(event) => setAttendedSessions(Number(event.target.value))} /></label>
          <button type="submit">Save Attended Count</button>
        </form>
      </section>
    )
  }

  if (view === 'portal' && activeRole) {
    return (
      <div className="portal-shell">
        <aside>
          <button className="portal-brand" onClick={logout}><span>FAMS</span></button>
          <p className="portal-label">{activeRole} Portal</p>
          <nav>
            {portals[activeRole].map(([title]) => (
              <button className={activeSection === title ? 'portal-nav active' : 'portal-nav'} onClick={() => openSection(title)} key={title}>{title}</button>
            ))}
          </nav>
          <button className="portal-logout" onClick={logout}>Logout</button>
        </aside>
        <main>
          <header className="portal-header">
            <div><p>Independent {activeRole} Workspace</p><h1>{activeRole} Dashboard</h1></div>
            <div className="profile"><span>{session?.username?.[0]?.toUpperCase()}</span><div><strong>{session?.username}</strong><small>{activeRole}</small></div></div>
          </header>
          {activeRole === 'Student' && (
            <section className="student-metrics">
              <article><small>Overall Attendance</small><strong>{attendance?.overall ?? 0}%</strong></article>
              <article><small>Conducted Sessions</small><strong>{attendance?.conducted ?? 0}</strong></article>
              <article><small>Attended Sessions</small><strong>{attendance?.attended ?? 0}</strong></article>
              <article><small>Academic Status</small><strong>{attendance?.eligible ? 'Eligible' : 'Shortage'}</strong></article>
            </section>
          )}
          {error && <div className="error">{error}</div>}
          {renderActiveSection()}
        </main>
      </div>
    )
  }

  return (
    <div className="public-site">
      <header className="public-header">
        <button className="public-brand" onClick={() => setView('home')}><span>FAMS</span><div><strong>Faculty Attendance</strong><small>Management System</small></div></button>
        <nav><a href="#academics">Academics</a><a href="#resources">Resources</a></nav>
        <button className="portal-button" onClick={() => document.getElementById('portals')?.scrollIntoView({ behavior: 'smooth' })}>Portal Login</button>
      </header>

      <section className="hero">
        <div>
          <p>Empowering Academic Excellence</p>
          <h1>Learning and Growth — Together</h1>
          <span>Reliable academic operations for students, faculty, department heads and administrators.</span>
          <div className="hero-actions"><button onClick={() => document.getElementById('academics')?.scrollIntoView({ behavior: 'smooth' })}>Explore Academics</button><button onClick={() => document.getElementById('portals')?.scrollIntoView({ behavior: 'smooth' })}>Access Portals</button></div>
        </div>
        <div className="hero-panel"><small>Academic Year 2026–27</small><strong>One campus. Every role. Clear academic records.</strong><div><span>Accredited Programs</span><b>24+</b></div><div><span>Students</span><b>3,200+</b></div></div>
      </section>

      <section id="academics" className="institution-info">
        <article><small>ACADEMICS</small><h2>Departments & Programs</h2><p>Discover structured course delivery, faculty guidance and transparent attendance records.</p></article>
        <article><small>STUDENT LIFE</small><h2>Learning Support</h2><p>Academic calendars, subject progress and personalized student information.</p></article>
        <article><small>INSTITUTION</small><h2>Responsible Governance</h2><p>Role-based academic administration and accurate institutional reporting.</p></article>
      </section>

      <section id="portals" className="portal-selection">
        <div className="section-heading"><p>Secure Access</p><h2>Choose your institutional portal</h2></div>
        {error && <div className="error">{error}</div>}
        <div className="portal-cards">
          {publicRoles.map((role) => (
            <article key={role}>
              <div className="portal-icon">{role.charAt(0)}</div>
              <h3>{role} Portal</h3>
              <p>{role === 'Student' ? 'View subject attendance and academic records.' : role === 'Faculty' ? 'Manage classes and attendance sessions.' : role === 'HOD' ? 'Lead department and institutional operations.' : 'Configure and administer institutional data.'}</p>
              {role === 'Student' ? (
                <div className="student-auth">
                  <input value={studentId} onChange={(event) => setStudentId(event.target.value.toUpperCase())} placeholder="25CSVVIT01" maxLength={11} />
                  <input type="password" value={studentPassword} onChange={(event) => setStudentPassword(event.target.value)} placeholder="8-character password" maxLength={8} />
                  {apiResult?.title === 'Signup successful' && <small className="signup-success">{apiResult.permissions}</small>}
                  <div className="auth-actions">
                    <button disabled={loading} onClick={() => login(role)}>{loading ? 'Signing in...' : 'Login'}</button>
                    <button disabled={loading} className="secondary" onClick={() => signupMode ? signupStudent() : setSignupMode(true)}>{signupMode ? 'Confirm Signup' : 'Signup'}</button>
                  </div>
                  {!signupMode && <button className="text-mode" onClick={() => setSignupMode(true)}>New student? Create individual account</button>}
                </div>
              ) : (
                roleLoginFor === role ? (
                  <div className="role-auth">
                    <input value={roleLoginId} onChange={(event) => setRoleLoginId(event.target.value)} placeholder={`${role} ID`} />
                    <input type="password" value={roleLoginPassword} onChange={(event) => setRoleLoginPassword(event.target.value)} placeholder="Password" />
                    <div className="auth-actions">
                      <button disabled={loading} onClick={() => login(role)}>{loading ? 'Signing in...' : 'Login'}</button>
                      <button className="secondary" onClick={() => { setRoleLoginFor(null); setError('') }}>Cancel</button>
                    </div>
                  </div>
                ) : (
                  <button disabled={loading} onClick={() => beginRoleLogin(role)}>{`Sign in as ${role}`}</button>
                )
              )}
            </article>
          ))}
        </div>
      </section>

      <footer id="resources"><span>© 2026 Faculty Attendance Management System</span><span>Admissions · Academics · Student Services · Contact</span></footer>
    </div>
  )
}

export default App
