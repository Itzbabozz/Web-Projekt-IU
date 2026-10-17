import { useCallback, useEffect, useState } from 'react';
import './App.css';

const API_BASE = 'http://localhost:8080/api';

async function api(path, options = {}) {
  const token = localStorage.getItem('token');
  const headers = { ...(options.headers || {}) };

  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  const contentType = response.headers.get('content-type') || '';
  const payload = contentType.includes('application/json') ? await response.json() : await response.text();

  if (!response.ok) {
    throw new Error(typeof payload === 'string' ? payload : payload.message || 'Request failed');
  }

  return payload;
}

function App() {
  const [tenantId, setTenantId] = useState('acme');
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [token, setToken] = useState(localStorage.getItem('token') || '');
  const [user, setUser] = useState(null);
  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);
  const [tasks, setTasks] = useState([]);
  const [users, setUsers] = useState([]);
  const [accountForm, setAccountForm] = useState({ username: '', password: '', role: 'EMPLOYEE' });
  const [projectForm, setProjectForm] = useState({ name: '', description: '', employeeIds: [] });
  const [taskForm, setTaskForm] = useState({ title: '', description: '', assigneeId: '', status: 'OPEN' });
  const [message, setMessage] = useState('');

  const selectedProject = projects.find((project) => project.id === selectedProjectId) || projects[0] || null;

  const loadCurrentUser = useCallback(async () => {
    try {
      const response = await api('/users/me');
      setUser(response);
      setTenantId(response.tenantId);
    } catch (error) {
      setMessage(error.message);
    }
  }, []);

  const loadProjects = useCallback(async () => {
    try {
      const response = await api('/projects');
      setProjects(response);
      if (response.length > 0 && !selectedProjectId) {
        setSelectedProjectId(response[0].id);
      }
    } catch (error) {
      setMessage(error.message);
    }
  }, [selectedProjectId]);

  const loadTasks = useCallback(async (projectId) => {
    try {
      const response = await api(`/projects/${projectId}/tasks`);
      setTasks(response);
    } catch (error) {
      setMessage(error.message);
    }
  }, []);

  const loadUsers = useCallback(async () => {
    try {
      const response = await api('/admin/users');
      setUsers(response);
    } catch (error) {
      setMessage(error.message);
    }
  }, []);

  useEffect(() => {
    if (!token) {
      setUser(null);
      setProjects([]);
      setTasks([]);
      return;
    }

    loadCurrentUser();
    loadProjects();
  }, [token, loadCurrentUser, loadProjects]);

  useEffect(() => {
    if (!token || !selectedProject) {
      setTasks([]);
      return;
    }
    loadTasks(selectedProject.id);
  }, [selectedProject, token, loadTasks]);

  useEffect(() => {
    if (token && user && user.role === 'ADMIN') {
      loadUsers();
    }
  }, [token, user, loadUsers]);

  async function handleLogin(event) {
    event.preventDefault();
    try {
      const result = await api('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password, tenantId }),
      });
      localStorage.setItem('token', result.token);
      setToken(result.token);
      setMessage('Login successful');
    } catch (error) {
      setMessage(error.message);
    }
  }

  function handleLogout() {
    localStorage.removeItem('token');
    setToken('');
    setUser(null);
    setProjects([]);
    setTasks([]);
    setUsers([]);
    setSelectedProjectId(null);
    setMessage('Logged out');
  }

  async function handleCreateUser(event) {
    event.preventDefault();
    try {
      await api('/admin/users', {
        method: 'POST',
        body: JSON.stringify({
          username: accountForm.username,
          password: accountForm.password,
          role: accountForm.role,
          tenantId,
        }),
      });
      setAccountForm({ username: '', password: '', role: 'EMPLOYEE' });
      setMessage('User created successfully');
      await loadUsers();
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function handleCreateProject(event) {
    event.preventDefault();
    try {
      const projectResponse = await api('/projects', {
        method: 'POST',
        body: JSON.stringify({
          name: projectForm.name,
          description: projectForm.description,
          employeeIds: projectForm.employeeIds,
          archived: false,
        }),
      });
      setProjectForm({ name: '', description: '', employeeIds: [] });
      setSelectedProjectId(projectResponse.id);
      setMessage('Project created successfully');
      await loadProjects();
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function handleCreateTask(event) {
    event.preventDefault();
    if (!selectedProject) return;
    try {
      await api(`/projects/${selectedProject.id}/tasks`, {
        method: 'POST',
        body: JSON.stringify({
          title: taskForm.title,
          description: taskForm.description,
          assigneeId: taskForm.assigneeId ? Number(taskForm.assigneeId) : null,
          status: taskForm.status,
          projectId: selectedProject.id,
        }),
      });
      setTaskForm({ title: '', description: '', assigneeId: '', status: 'OPEN' });
      setMessage('Task created successfully');
      await loadTasks(selectedProject.id);
      await loadProjects();
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function handleStatusUpdate(taskId, nextStatus) {
    try {
      await api(`/tasks/${taskId}/status?status=${nextStatus}`, {
        method: 'PATCH',
      });
      setMessage('Task status updated');
      await loadTasks(selectedProject.id);
      await loadProjects();
    } catch (error) {
      setMessage(error.message);
    }
  }

  const canManageUsers = user && user.role === 'ADMIN';
  const canManageProjects = user && (user.role === 'ADMIN' || user.role === 'PROJECT_MANAGER');

  if (!token) {
    return (
      <div className="page-shell login-shell">
        <div className="panel login-panel">
          <h1>ProjectFlow</h1>
          <p>Multi-tenant project management</p>
          <form onSubmit={handleLogin} className="stacked-form">
            <label>
              Tenant
              <input value={tenantId} onChange={(event) => setTenantId(event.target.value)} />
            </label>
            <label>
              Username
              <input value={username} onChange={(event) => setUsername(event.target.value)} />
            </label>
            <label>
              Password
              <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
            </label>
            <button type="submit">Sign in</button>
          </form>
          <small>Demo users: admin / pm1 / employee1 in tenant acme</small>
          {message && <div className="message">{message}</div>}
        </div>
      </div>
    );
  }

  return (
    <div className="page-shell dashboard-shell">
      <aside className="sidebar panel">
        <div className="brand-row">
          <h2>ProjectFlow</h2>
          <button className="ghost-button" onClick={handleLogout}>Logout</button>
        </div>

        <div className="profile-box">
          <strong>{user?.username}</strong>
          <span>{user?.role}</span>
          <span>Tenant: {user?.tenantId}</span>
        </div>

        <div className="section-title">Projects</div>
        <div className="project-list">
          {projects.map((project) => (
            <button
              key={project.id}
              className={`project-item ${selectedProject?.id === project.id ? 'selected' : ''}`}
              onClick={() => setSelectedProjectId(project.id)}
            >
              <span>{project.name}</span>
              <small>{project.progress}% complete</small>
            </button>
          ))}
        </div>

        {canManageProjects && (
          <form onSubmit={handleCreateProject} className="stacked-form compact-form">
            <div className="section-title">New project</div>
            <label>
              Name
              <input value={projectForm.name} onChange={(event) => setProjectForm({ ...projectForm, name: event.target.value })} />
            </label>
            <label>
              Description
              <textarea value={projectForm.description} onChange={(event) => setProjectForm({ ...projectForm, description: event.target.value })} />
            </label>
            <label>
              Assign employees
              <select
                multiple
                value={projectForm.employeeIds}
                onChange={(event) => {
                  const selected = Array.from(event.target.selectedOptions, (option) => Number(option.value));
                  setProjectForm({ ...projectForm, employeeIds: selected });
                }}
              >
                {users.map((person) => (
                  <option key={person.id} value={person.id}>{person.username}</option>
                ))}
              </select>
            </label>
            <button type="submit">Create project</button>
          </form>
        )}

        {canManageUsers && (
          <form onSubmit={handleCreateUser} className="stacked-form compact-form">
            <div className="section-title">Create user</div>
            <label>
              Username
              <input value={accountForm.username} onChange={(event) => setAccountForm({ ...accountForm, username: event.target.value })} />
            </label>
            <label>
              Password
              <input type="password" value={accountForm.password} onChange={(event) => setAccountForm({ ...accountForm, password: event.target.value })} />
            </label>
            <label>
              Role
              <select value={accountForm.role} onChange={(event) => setAccountForm({ ...accountForm, role: event.target.value })}>
                <option value="EMPLOYEE">Employee</option>
                <option value="PROJECT_MANAGER">Project Manager</option>
                <option value="ADMIN">Administrator</option>
              </select>
            </label>
            <button type="submit">Add user</button>
          </form>
        )}
      </aside>

      <main className="content panel">
        {selectedProject ? (
          <>
            <header className="project-header">
              <div>
                <span className="eyebrow">Project</span>
                <h1>{selectedProject.name}</h1>
              </div>
              <div className="progress-badge">{selectedProject.progress}% complete</div>
            </header>

            <p className="project-description">{selectedProject.description}</p>

            <div className="meta-grid">
              <div className="meta-card">
                <span>Assigned employees</span>
                <strong>{selectedProject.employees?.map((employee) => employee.username).join(', ') || 'None'}</strong>
              </div>
              <div className="meta-card">
                <span>Project status</span>
                <strong>{selectedProject.archived ? 'Archived' : 'Active'}</strong>
              </div>
            </div>

            <form onSubmit={handleCreateTask} className="stacked-form task-form">
              <div className="section-title">Create task</div>
              <div className="inline-grid">
                <label>
                  Title
                  <input value={taskForm.title} onChange={(event) => setTaskForm({ ...taskForm, title: event.target.value })} />
                </label>
                <label>
                  Status
                  <select value={taskForm.status} onChange={(event) => setTaskForm({ ...taskForm, status: event.target.value })}>
                    <option value="OPEN">Open</option>
                    <option value="IN_PROGRESS">In Progress</option>
                    <option value="COMPLETED">Completed</option>
                  </select>
                </label>
              </div>
              <label>
                Description
                <textarea value={taskForm.description} onChange={(event) => setTaskForm({ ...taskForm, description: event.target.value })} />
              </label>
              <label>
                Assign to
                <select value={taskForm.assigneeId} onChange={(event) => setTaskForm({ ...taskForm, assigneeId: event.target.value })}>
                  <option value="">Unassigned</option>
                  {selectedProject.employees?.map((employee) => (
                    <option key={employee.id} value={employee.id}>{employee.username}</option>
                  ))}
                </select>
              </label>
              <button type="submit">Add task</button>
            </form>

            <div className="section-title">Tasks</div>
            <div className="task-list">
              {tasks.length === 0 ? (
                <div className="empty-state">No tasks yet</div>
              ) : (
                tasks.map((task) => (
                  <div key={task.id} className="task-card">
                    <div>
                      <h3>{task.title}</h3>
                      <p>{task.description}</p>
                    </div>
                    <div className="task-meta">
                      <span>Owner: {task.assigneeName || 'Unassigned'}</span>
                      <span>Status: {task.status}</span>
                      <select
                        value={task.status}
                        onChange={(event) => handleStatusUpdate(task.id, event.target.value)}
                      >
                        <option value="OPEN">Open</option>
                        <option value="IN_PROGRESS">In Progress</option>
                        <option value="COMPLETED">Completed</option>
                      </select>
                    </div>
                  </div>
                ))
              )}
            </div>
          </>
        ) : (
          <div className="empty-state">No project selected</div>
        )}

        {message && <div className="message">{message}</div>}
      </main>
    </div>
  );
}

export default App;
