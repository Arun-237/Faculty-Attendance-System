import { useEffect, useRef, useState } from 'react'
import heroImage from './assets/campus-hero.png'
import './App.css'

const accounts = {
  Administrator: { username: 'Admin', password: 'ADMINVVIT' },
  HOD: { username: 'HOD', password: 'HODVVIT' },
  Faculty: { username: 'Faculty1', password: 'VVIT' },
}

const publicRoles = ['Administrator', 'HOD', 'Faculty', 'Student']

const portals = {
  Administrator: [
    ['Attendance', 'Mark or correct date-wise student attendance'],
    ['Attendance Report', 'View date-wise and subject-wise student attendance'],
  ],
  HOD: [
    ['Attendance Report', 'View date-wise and subject-wise student attendance'],
  ],
  Faculty: [
    ['Attendance', 'Mark date-wise subject attendance'],
    ['Attendance Report', 'View date-wise and subject-wise student attendance'],
  ],
  Student: [
    ['My Subjects', 'Subject-wise attendance summary'],
  ],
}

const attendanceSuccessTitles = new Set([
  'Attendance updated',
  'Students attendance submitted successfully',
  'All students attendance marked present',
])
const portalSessionKey = 'fas-portal-session'

function getSavedPortalSession() {
  try {
    const storedSession = sessionStorage.getItem(portalSessionKey)
    if (!storedSession) return null
    const saved = JSON.parse(storedSession)
    if (!saved.activeRole || !saved.credentials || !portals[saved.activeRole]) return null
    return saved
  } catch {
    return null
  }
}

function App() {
  const savedPortalSession = getSavedPortalSession()
  const [view, setView] = useState(savedPortalSession ? 'portal' : 'home')
  const [activeRole, setActiveRole] = useState(savedPortalSession?.activeRole || null)
  const [activeSection, setActiveSection] = useState(savedPortalSession?.activeSection || null)
  const [session, setSession] = useState(savedPortalSession?.session || null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [apiResult, setApiResult] = useState(null)
  const [attendance, setAttendance] = useState(null)
  const [studentId, setStudentId] = useState('')
  const [studentName, setStudentName] = useState('')
  const [studentEmail, setStudentEmail] = useState('')
  const [studentPassword, setStudentPassword] = useState('')
  const [signupMode, setSignupMode] = useState(false)
  const [portalCredentials, setPortalCredentials] = useState(savedPortalSession?.credentials || null)
  const [attendanceList, setAttendanceList] = useState([])
  const [selectedStudent, setSelectedStudent] = useState('')
  const [selectedSubject, setSelectedSubject] = useState('DSA')
  const [conductedSessions, setConductedSessions] = useState(50)
  const [attendedSessions, setAttendedSessions] = useState(30)
  const [roleLoginFor, setRoleLoginFor] = useState(null)
  const [roleLoginId, setRoleLoginId] = useState('')
  const [roleLoginPassword, setRoleLoginPassword] = useState('')
  const [selectedClass, setSelectedClass] = useState('Class-A')
  const [attendanceDate, setAttendanceDate] = useState(new Date().toISOString().slice(0, 10))
  const [dailySubject, setDailySubject] = useState('DSA')
  const [classStudents, setClassStudents] = useState([])
  const [attendanceStatus, setAttendanceStatus] = useState({})
  const [attendanceReport, setAttendanceReport] = useState([])
  const [studentFilterDate, setStudentFilterDate] = useState('')
  const [studentFilterSubject, setStudentFilterSubject] = useState('')
  const [reportFilterDate, setReportFilterDate] = useState('')
  const [reportFilterSubject, setReportFilterSubject] = useState('')
  const [reportFilterClass, setReportFilterClass] = useState('')
  const [hodReportMode, setHodReportMode] = useState('Daily')
  const [reportStatus, setReportStatus] = useState({})
  const successMessageRef = useRef(null)
  const errorMessageRef = useRef(null)

  useEffect(() => {
    restorePortalSession()
  }, [])

  useEffect(() => {
    if (attendanceSuccessTitles.has(apiResult?.title)) {
      successMessageRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' })
      successMessageRef.current?.focus({ preventScroll: true })
    }
  }, [apiResult])

  useEffect(() => {
    if (error) {
      errorMessageRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' })
      errorMessageRef.current?.focus({ preventScroll: true })
    }
  }, [error])

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
      setError('Invalid user mail/id or password')
      return
    }
    setLoading(true)
    setError('')
    try {
      const credentials = btoa(`${enteredId}:${enteredPassword}`)
      const response = await fetch('http://localhost:8080/api/me', {
        headers: { Authorization: `Basic ${credentials}` },
      })
      if (!response.ok) throw new Error('Invalid user mail/id or password')
      const sessionDetails = await response.json()
      setSession(sessionDetails)
      setActiveRole(role)
      setActiveSection(portals[role][0][0])
      setPortalCredentials(credentials)
      sessionStorage.setItem(portalSessionKey, JSON.stringify({
        activeRole: role,
        activeSection: portals[role][0][0],
        credentials,
        session: sessionDetails,
      }))
      if (role !== 'Student') {
        if (role === 'HOD') await loadHodReport(credentials)
        if (role === 'Faculty' || role === 'Administrator') {
          await loadClassStudents(selectedClass, credentials)
          await loadPortalReport(credentials)
        }
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
      const credentials = btoa(`${studentEmail.trim().toLowerCase()}:${studentPassword}`)
      const response = await fetch('http://localhost:8080/api/me', {
        headers: { Authorization: `Basic ${credentials}` },
      })
      if (!response.ok) throw new Error('Invalid user mail/id or password')
      const sessionDetails = await response.json()
      setSession(sessionDetails)
      setActiveRole('Student')
      setActiveSection(portals.Student[0][0])
      setPortalCredentials(credentials)
      sessionStorage.setItem(portalSessionKey, JSON.stringify({
        activeRole: 'Student',
        activeSection: portals.Student[0][0],
        credentials,
        session: sessionDetails,
      }))
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
        body: JSON.stringify({ studentId, password: studentPassword, name: studentName, email: studentEmail }),
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
    sessionStorage.removeItem(portalSessionKey)
    setSession(null)
    setActiveRole(null)
    setActiveSection(null)
    setApiResult(null)
    setView('home')
    setAttendance(null)
    setPortalCredentials(null)
    setStudentId('')
    setStudentName('')
    setStudentEmail('')
    setStudentPassword('')
  }

  async function restorePortalSession() {
    const storedSession = sessionStorage.getItem(portalSessionKey)
    if (!storedSession) return

    try {
      const saved = JSON.parse(storedSession)
      if (!saved.activeRole || !saved.credentials || !portals[saved.activeRole]) {
        throw new Error('Invalid saved portal session')
      }
      const response = await fetch('http://localhost:8080/api/me', {
        headers: { Authorization: `Basic ${saved.credentials}` },
      })
      if (!response.ok) throw new Error('Portal session expired')

      const sessionDetails = await response.json()
      setSession(sessionDetails)
      setActiveRole(saved.activeRole)
      setActiveSection(saved.activeSection || portals[saved.activeRole][0][0])
      setPortalCredentials(saved.credentials)
      setView('portal')

      if (saved.activeRole === 'Student') {
        await loadStudentAttendance(saved.credentials)
      } else if (saved.activeRole === 'HOD') {
        await loadHodReport(saved.credentials)
      } else {
        await loadClassStudents(selectedClass, saved.credentials, saved.activeRole)
        await loadPortalReport(saved.credentials, reportFilterDate, reportFilterSubject, reportFilterClass, saved.activeRole)
      }
    } catch {
      sessionStorage.removeItem(portalSessionKey)
      setView('home')
      setActiveRole(null)
      setActiveSection(null)
      setSession(null)
      setPortalCredentials(null)
    }
  }

  async function loadStudentAttendance(credentials, filterDate = studentFilterDate, filterSubject = studentFilterSubject) {
    const params = new URLSearchParams()
    if (filterDate) params.set('date', filterDate)
    if (filterSubject) params.set('subject', filterSubject)
    const response = await fetch(`http://localhost:8080/api/student/attendance${params.size ? `?${params}` : ''}`, {
      headers: { Authorization: `Basic ${credentials}` },
    })
    if (response.ok) setAttendance(await response.json())
  }

  async function loadClassStudents(className, credentials = portalCredentials, roleOverride = activeRole) {
    if (!credentials) return
    const role = roleOverride === 'Administrator' ? 'administrator' : 'faculty'
    const response = await fetch(`http://localhost:8080/api/${role}/students?section=${encodeURIComponent(className)}`, {
      headers: { Authorization: `Basic ${credentials}` },
    })
    if (response.ok) {
      const students = await response.json()
      setClassStudents(students)
      setAttendanceStatus(Object.fromEntries(students.map((student) => [student.studentId, 'P'])))
    }
  }

  async function loadHodReport(credentials = portalCredentials, date = reportFilterDate, subject = reportFilterSubject, section = reportFilterClass, mode = hodReportMode) {
    if (!credentials) return
    const params = new URLSearchParams()
    if (date && mode === 'Daily') params.set('date', date)
    if (subject) params.set('subject', subject)
    if (section) params.set('section', section)
    const endpoint = mode === 'Overall'
      ? '/api/hod/attendance/overall'
      : '/api/hod/attendance/report'
    const response = await fetch(`http://localhost:8080${endpoint}${params.size ? `?${params}` : ''}`, {
      headers: { Authorization: `Basic ${credentials}` },
    })
    if (response.ok) setAttendanceReport(await response.json())
  }

  async function loadPortalReport(credentials = portalCredentials, date = reportFilterDate, subject = reportFilterSubject, section = reportFilterClass, roleOverride = activeRole) {
    if (!credentials) return
    const params = new URLSearchParams()
    if (date) params.set('date', date)
    if (subject) params.set('subject', subject)
    if (section) params.set('section', section)
    const role = roleOverride === 'Administrator' ? 'administrator' : 'faculty'
    const response = await fetch(`http://localhost:8080/api/${role}/attendance/report${params.size ? `?${params}` : ''}`, {
      headers: { Authorization: `Basic ${credentials}` },
    })
    if (response.ok) setAttendanceReport(await response.json())
  }

  async function saveReportRecord(record) {
    const status = reportStatus[`${record.studentId}-${record.date}-${record.subject}`] || record.status
    try {
      const response = await fetch('http://localhost:8080/api/administrator/students/attendance/daily', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Basic ${portalCredentials}`,
        },
        body: JSON.stringify({
          date: record.date,
          subject: record.subject,
          records: [{ studentId: record.studentId, status }],
        }),
      })
      const result = await response.json()
      if (!response.ok) throw new Error(result.message || 'Attendance update failed')
      setApiResult({ title: 'Attendance updated', permissions: result.message })
      await loadPortalReport(portalCredentials)
    } catch (saveError) {
      setError(saveError.message)
    }
  }

  async function saveDailyAttendance() {
    if (!attendanceDate) {
      setError('Select an attendance date')
      return
    }
    if (classStudents.length === 0) {
      setError(`No students found in ${selectedClass}`)
      return
    }
    const role = activeRole === 'Administrator' ? 'administrator' : 'faculty'
    try {
      const response = await fetch(`http://localhost:8080/api/${role}/students/attendance/daily`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Basic ${portalCredentials}`,
        },
        body: JSON.stringify({
          date: attendanceDate,
          subject: dailySubject,
          records: classStudents.map((student) => ({
            studentId: student.studentId,
            status: attendanceStatus[student.studentId] || 'P',
          })),
        }),
      })
      const result = await response.json()
      if (!response.ok) throw new Error(result.message || 'Attendance update failed')
      setApiResult({ title: 'Students attendance submitted successfully', permissions: result.message })
      setError('')
    } catch (saveError) {
      setApiResult(null)
      setError(saveError.message)
    }
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
    const storedSession = sessionStorage.getItem(portalSessionKey)
    if (storedSession) {
      try {
        const saved = JSON.parse(storedSession)
        sessionStorage.setItem(portalSessionKey, JSON.stringify({ ...saved, activeSection: title }))
      } catch {
        sessionStorage.removeItem(portalSessionKey)
      }
    }
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
      if (activeRole === 'HOD') {
        await loadHodReport(credentials)
      } else if (activeRole === 'Faculty' || activeRole === 'Administrator') {
        await loadClassStudents(selectedClass, credentials)
        await loadPortalReport(credentials)
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
          <div className="working-form attendance-editor">
            <label>Date<input type="date" value={studentFilterDate} onChange={(event) => setStudentFilterDate(event.target.value)} /></label>
            <label>Subject<select value={studentFilterSubject} onChange={(event) => setStudentFilterSubject(event.target.value)}><option value="">All Subjects</option><option>DSA</option><option>DBMS</option><option>OS</option><option>Python</option><option>GenAI</option></select></label>
            <button type="button" onClick={() => loadStudentAttendance(portalCredentials)}>Filter Attendance</button>
            <button type="button" className="text-mode" onClick={() => { setStudentFilterDate(''); setStudentFilterSubject(''); loadStudentAttendance(portalCredentials, '', '') }}>Clear Filters</button>
          </div>
          <table className="data-table"><thead><tr><th>Date</th><th>Subject</th><th>Status</th></tr></thead><tbody>
            {(attendance?.subjects || []).map((record) => (
              <tr key={`${record.date}-${record.subject}`}><td>{record.date}</td><td>{record.subject}</td><td>{record.status === 'P' ? 'Present' : 'Absent'}</td></tr>
            ))}
          </tbody></table>
        </section>
      )
    }

    if (activeRole === 'Faculty' || activeRole === 'Administrator') {
      const showAttendanceOperations = activeSection === 'Attendance'
      return (
        <section className="working-section">
          <h2>{activeSection}</h2>
          {showAttendanceOperations && (
            <>
              <div className="working-form attendance-editor">
                <label>Class<select value={selectedClass} onChange={(event) => { setSelectedClass(event.target.value); loadClassStudents(event.target.value) }}><option>Class-A</option><option>Class-B</option></select></label>
                <label>Date<input type="date" value={attendanceDate} onChange={(event) => setAttendanceDate(event.target.value)} /></label>
                <label>Subject<select value={dailySubject} onChange={(event) => setDailySubject(event.target.value)}><option>DSA</option><option>DBMS</option><option>OS</option><option>Python</option><option>GenAI</option></select></label>
              </div>
              <table className="data-table"><thead><tr><th>Student ID</th><th>Name</th><th>Section</th><th>Attendance</th></tr></thead><tbody>
                {classStudents.map((student) => (
                  <tr key={student.studentId}>
                    <td>{student.studentId}</td><td>{student.name || '-'}</td><td>{student.section}</td>
                    <td><select value={attendanceStatus[student.studentId] || 'P'} onChange={(event) => setAttendanceStatus((current) => ({ ...current, [student.studentId]: event.target.value }))}><option value="P">P - Present</option><option value="AB">AB - Absent</option></select></td>
                  </tr>
                ))}
              </tbody></table>
              <button
                type="button"
                className="text-mode"
                onClick={() => {
                  setAttendanceStatus(Object.fromEntries(classStudents.map((student) => [student.studentId, 'P'])))
                  setApiResult({ title: 'All students attendance marked present', permissions: 'All students are marked as present. Click Save Attendance to submit.' })
                  setError('')
                }}
              >
                Select All Present
              </button>
              <button type="button" onClick={saveDailyAttendance}>Save Attendance</button>
            </>
          )}
          {activeSection === 'Attendance Report' && (
            <>
              <h3>Attendance Report</h3>
              <div className="working-form attendance-editor">
                <label>Date<input type="date" value={reportFilterDate} onChange={(event) => setReportFilterDate(event.target.value)} /></label>
                <label>Subject<select value={reportFilterSubject} onChange={(event) => setReportFilterSubject(event.target.value)}><option value="">All Subjects</option><option>DSA</option><option>DBMS</option><option>OS</option><option>Python</option><option>GenAI</option></select></label>
                <label>Class<select value={reportFilterClass} onChange={(event) => setReportFilterClass(event.target.value)}><option value="">All Classes</option><option>Class-A</option><option>Class-B</option></select></label>
                <button type="button" onClick={() => loadPortalReport(portalCredentials)}>Filter Report</button>
                <button type="button" className="text-mode" onClick={() => { setReportFilterDate(''); setReportFilterSubject(''); setReportFilterClass(''); loadPortalReport(portalCredentials, '', '', '') }}>Clear Filters</button>
              </div>
              <table className="data-table"><thead><tr><th>Date</th><th>Student ID</th><th>Name</th><th>Class</th><th>Subject</th><th>Status</th>{activeRole === 'Administrator' && <th>Update</th>}</tr></thead><tbody>
                {attendanceReport.map((record) => {
                  const key = `${record.studentId}-${record.date}-${record.subject}`
                  return (
                    <tr key={key}><td>{record.date}</td><td>{record.studentId}</td><td>{record.name || '-'}</td><td>{record.section}</td><td>{record.subject}</td>
                      <td>{activeRole === 'Administrator' ? (
                        <select value={reportStatus[key] || record.status} onChange={(event) => setReportStatus((current) => ({ ...current, [key]: event.target.value }))}><option value="P">P - Present</option><option value="AB">AB - Absent</option></select>
                      ) : (record.status === 'P' ? 'Present' : 'Absent')}</td>
                      {activeRole === 'Administrator' && <td><button type="button" onClick={() => saveReportRecord(record)}>Save</button></td>}
                    </tr>
                  )
                })}
              </tbody></table>
            </>
          )}
        </section>
      )
    }

    if (activeRole === 'HOD') {
      return (
        <section className="working-section">
          <h2>{activeSection}</h2>
          <div className="working-form attendance-editor">
            <label>Report<select value={hodReportMode} onChange={(event) => { setHodReportMode(event.target.value); loadHodReport(portalCredentials, reportFilterDate, reportFilterSubject, reportFilterClass, event.target.value) }}><option>Daily</option><option>Overall</option></select></label>
            {hodReportMode === 'Daily' && <label>Date<input type="date" value={reportFilterDate} onChange={(event) => setReportFilterDate(event.target.value)} /></label>}
            <label>Subject<select value={reportFilterSubject} onChange={(event) => setReportFilterSubject(event.target.value)}><option value="">All Subjects</option><option>DSA</option><option>DBMS</option><option>OS</option><option>Python</option><option>GenAI</option></select></label>
            <label>Class<select value={reportFilterClass} onChange={(event) => setReportFilterClass(event.target.value)}><option value="">All Classes</option><option>Class-A</option><option>Class-B</option></select></label>
            <button type="button" onClick={() => loadHodReport(portalCredentials)}>Filter Report</button>
            <button type="button" className="text-mode" onClick={() => { setReportFilterDate(''); setReportFilterSubject(''); setReportFilterClass(''); loadHodReport(portalCredentials, '', '', '', hodReportMode) }}>Clear Filters</button>
          </div>
          {hodReportMode === 'Daily' ? (
            <table className="data-table"><thead><tr><th>Date</th><th>Student ID</th><th>Name</th><th>Section</th><th>Subject</th><th>Status</th></tr></thead><tbody>
              {attendanceReport.map((record) => (
                <tr key={`${record.studentId}-${record.date}-${record.subject}`}><td>{record.date}</td><td>{record.studentId}</td><td>{record.name || '-'}</td><td>{record.section}</td><td>{record.subject}</td><td>{record.status === 'P' ? 'Present' : 'Absent'}</td></tr>
              ))}
            </tbody></table>
          ) : (
            <table className="data-table"><thead><tr><th>Student ID</th><th>Name</th><th>Section</th><th>Conducted</th><th>Present</th><th>Absent</th><th>Overall</th></tr></thead><tbody>
              {attendanceReport.map((record) => (
                <tr key={record.studentId}><td>{record.studentId}</td><td>{record.name || '-'}</td><td>{record.section}</td><td>{record.conducted}</td><td>{record.attended}</td><td>{record.absent}</td><td>{record.overall}%</td></tr>
              ))}
            </tbody></table>
          )}
        </section>
      )
    }

    return null
  }

  if (view === 'portal' && activeRole) {
    return (
      <div className="portal-shell">
        <aside>
          <div className="portal-brand-text">Faculty Attendance</div>
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
            <div className="profile"><span>{(session?.name || session?.username)?.[0]?.toUpperCase()}</span><div><strong>{session?.name || session?.username}</strong><small>{session?.username} · {activeRole}</small></div></div>
          </header>
          {activeRole === 'Student' && (
            <section className="student-metrics">
              <article><small>Overall Attendance</small><strong>{attendance?.overall ?? 0}%</strong></article>
              <article><small>Conducted Sessions</small><strong>{attendance?.conducted ?? 0}</strong></article>
              <article><small>Attended Sessions</small><strong>{attendance?.attended ?? 0}</strong></article>
              <article><small>Academic Status</small><strong>{attendance?.eligible ? 'Eligible' : 'Shortage'}</strong></article>
            </section>
          )}
          {error && <div ref={errorMessageRef} className="error" role="alert" tabIndex="-1">{error}</div>}
          {attendanceSuccessTitles.has(apiResult?.title) && (
            <div ref={successMessageRef} className="success" role="status" aria-live="polite" tabIndex="-1">
              <strong>{apiResult.title}</strong>
              {apiResult.permissions && <span>{apiResult.permissions}</span>}
            </div>
          )}
          {renderActiveSection()}
        </main>
      </div>
    )
  }

  return (
    <div className="public-site">
      <header className="public-header">
        <div className="public-brand"><strong>Faculty Attendance</strong><small>Management System</small></div>
        <nav><a href="#academics">Academics</a><a href="#resources">Resources</a></nav>
        <button className="portal-button" onClick={() => document.getElementById('portals')?.scrollIntoView({ behavior: 'smooth' })}>Portal Login</button>
      </header>

      <section className="hero">
        <div>
          <p className="hero-badge">Faculty Attendance System</p>
          <h1>Every class, <em>accounted for.</em></h1>
          <span>Faculty mark attendance in seconds. Students see where they stand. Administrators and HODs see the whole department at a glance.</span>
          <div className="hero-actions"><button onClick={() => document.getElementById('portals')?.scrollIntoView({ behavior: 'smooth' })}>Get started</button><button onClick={() => document.getElementById('academics')?.scrollIntoView({ behavior: 'smooth' })}>Explore academics</button></div>
        </div>
        <div className="hero-visual">
          <div className="hero-image-accent" aria-hidden="true"></div>
          <img src={heroImage} alt="VVIT university campus building" />
        </div>
      </section>

      <section id="academics" className="institution-info">
        <article><small>ACADEMICS</small><h2>Departments & Programs</h2><p>Discover structured course delivery, faculty guidance and transparent attendance records.</p></article>
        <article><small>STUDENT LIFE</small><h2>Learning Support</h2><p>Academic calendars, subject progress and personalized student information.</p></article>
        <article><small>INSTITUTION</small><h2>Responsible Governance</h2><p>Role-based academic administration and accurate institutional reporting.</p></article>
      </section>

      <section id="portals" className="portal-selection">
        <div className="section-heading"><p>Secure Access</p><h2>Choose your institutional portal</h2></div>
        {error && <div ref={errorMessageRef} className="error" role="alert" tabIndex="-1">{error}</div>}
        <div className="portal-cards">
          {publicRoles.map((role) => (
            <article key={role}>
              <div className="portal-icon">{role.charAt(0)}</div>
              <h3>{role} Portal</h3>
              <p>{role === 'Student' ? 'View subject attendance and academic records.' : role === 'Faculty' ? 'Manage classes and attendance sessions.' : role === 'HOD' ? 'Lead department and institutional operations.' : 'Configure and administer institutional data.'}</p>
              {role === 'Student' ? (
                <div className="student-auth">
                  {signupMode ? (
                    <>
                      <input value={studentId} onChange={(event) => setStudentId(event.target.value.toUpperCase())} placeholder="Student ID: 25CSVVIT01" maxLength={11} />
                      <input value={studentName} onChange={(event) => setStudentName(event.target.value)} placeholder="Student name" maxLength={100} />
                      <input type="email" value={studentEmail} onChange={(event) => setStudentEmail(event.target.value)} placeholder="Email address" maxLength={255} />
                    </>
                  ) : (
                    <input type="email" value={studentEmail} onChange={(event) => setStudentEmail(event.target.value)} placeholder="Email address" maxLength={255} />
                  )}
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
