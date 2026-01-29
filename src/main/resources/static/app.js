/* Minimal vanilla JS client for the UE SMS API.
   This is intentionally simple (no frameworks) so you can extend it easily. */

const API = ""; // same origin

function getToken() {
  return localStorage.getItem("ue_sms_token");
}
function setToken(token) {
  localStorage.setItem("ue_sms_token", token);
}
function clearAuth() {
  localStorage.removeItem("ue_sms_token");
  localStorage.removeItem("ue_sms_role");
}
function setRole(role) {
  localStorage.setItem("ue_sms_role", role);
}
function getRole() {
  return localStorage.getItem("ue_sms_role");
}

async function apiFetch(path, opts = {}) {
  const headers = opts.headers || {};
  headers["Content-Type"] = "application/json";

  const token = getToken();
  if (token) {
    headers["Authorization"] = "Bearer " + token;
  }

  const res = await fetch(API + path, { ...opts, headers });
  const text = await res.text();
  let body = null;
  try { body = text ? JSON.parse(text) : null; } catch { body = text; }

  if (!res.ok) {
    const msg = body?.message || body?.error || ("HTTP " + res.status);
    throw new Error(msg);
  }
  return body;
}

function el(id) { return document.getElementById(id); }

function showMsg(targetId, message, ok = true) {
  const target = el(targetId);
  if (!target) return;
  target.className = "msg " + (ok ? "ok" : "err");
  target.textContent = message;
}

async function loadDepartmentsInto(selectEl) {
  const departments = await apiFetch("/api/departments", { method: "GET" });
  selectEl.innerHTML = "";
  for (const d of departments) {
    const opt = document.createElement("option");
    opt.value = d.id;
    opt.textContent = `${d.code} - ${d.name}`;
    selectEl.appendChild(opt);
  }
}

/* -------- index.html (register + login) -------- */
async function initIndex() {
  // Populate departments for sign-up
  try {
    await loadDepartmentsInto(el("deptSelect"));
  } catch (e) {
    showMsg("registerMsg", "Could not load departments: " + e.message, false);
  }

  el("registerForm")?.addEventListener("submit", async (evt) => {
    evt.preventDefault();
    const fd = new FormData(evt.target);
    const payload = Object.fromEntries(fd.entries());
    payload.departmentId = Number(payload.departmentId);

    try {
      const auth = await apiFetch("/api/auth/register", {
        method: "POST",
        body: JSON.stringify(payload)
      });
      setToken(auth.token);
      setRole(auth.role);
      showMsg("registerMsg", "Registered successfully. Redirecting...", true);
      window.location.href = "/student.html";
    } catch (e) {
      showMsg("registerMsg", e.message, false);
    }
  });

  el("loginForm")?.addEventListener("submit", async (evt) => {
    evt.preventDefault();
    const fd = new FormData(evt.target);
    const payload = Object.fromEntries(fd.entries());

    try {
      const auth = await apiFetch("/api/auth/login", {
        method: "POST",
        body: JSON.stringify(payload)
      });
      setToken(auth.token);
      setRole(auth.role);
      showMsg("loginMsg", "Login OK. Redirecting...", true);
      if (auth.role === "TEACHER") {
        window.location.href = "/teacher.html";
      } else {
        window.location.href = "/student.html";
      }
    } catch (e) {
      showMsg("loginMsg", e.message, false);
    }
  });
}

/* -------- student.html -------- */
function renderTable(headers, rows) {
  const table = document.createElement("table");
  const thead = document.createElement("thead");
  const trh = document.createElement("tr");
  for (const h of headers) {
    const th = document.createElement("th");
    th.textContent = h;
    trh.appendChild(th);
  }
  thead.appendChild(trh);
  table.appendChild(thead);

  const tbody = document.createElement("tbody");
  for (const row of rows) {
    const tr = document.createElement("tr");
    for (const cell of row) {
      const td = document.createElement("td");
      if (cell instanceof Node) td.appendChild(cell);
      else td.textContent = String(cell ?? "");
      tr.appendChild(td);
    }
    tbody.appendChild(tr);
  }
  table.appendChild(tbody);
  return table;
}

async function refreshStudentProfile() {
  const s = await apiFetch("/api/students/me", { method: "GET" });
  el("studentProfile").innerHTML = `
    <div><b>${s.fullName}</b> (${s.studentNo})</div>
    <div>Email: ${s.email}</div>
    <div>Department: ${s.department.code} - ${s.department.name}</div>
    <div>Status: ${s.status}</div>
  `;
}

async function refreshCourseCatalog() {
  const courses = await apiFetch("/api/courses", { method: "GET" });
  const rows = courses.map(c => {
    const btn = document.createElement("button");
    btn.textContent = "Enroll";
    btn.addEventListener("click", async () => {
      try {
        await apiFetch("/api/enrollments/me", {
          method: "POST",
          body: JSON.stringify({ courseId: c.id })
        });
        showMsg("courseMsg", "Enrolled in " + c.code, true);
        await refreshMyEnrollments();
        await refreshCourseCatalog();
      } catch (e) {
        showMsg("courseMsg", e.message, false);
      }
    });
    return [
      c.code,
      c.title,
      c.credit,
      c.department.code,
      `${c.currentlyEnrolled}/${c.capacity}`,
      btn
    ];
  });

  const table = renderTable(["Code", "Title", "Credit", "Dept", "Enrolled/Cap", "Action"], rows);
  el("courseList").innerHTML = "";
  el("courseList").appendChild(table);
}

async function refreshMyEnrollments() {
  const enrollments = await apiFetch("/api/enrollments/me", { method: "GET" });
  const rows = enrollments.map(e => {
    const btn = document.createElement("button");
    btn.className = "danger";
    btn.textContent = "Drop";
    btn.disabled = e.status !== "ENROLLED";
    btn.addEventListener("click", async () => {
      try {
        await apiFetch("/api/enrollments/me/" + e.id, { method: "DELETE" });
        showMsg("enrollmentMsg", "Dropped " + e.courseCode, true);
        await refreshMyEnrollments();
        await refreshCourseCatalog();
      } catch (err) {
        showMsg("enrollmentMsg", err.message, false);
      }
    });

    return [
      e.courseCode,
      e.courseTitle,
      e.status,
      e.grade || "-",
      new Date(e.enrolledAt).toLocaleString(),
      btn
    ];
  });

  const table = renderTable(["Course", "Title", "Status", "Grade", "Enrolled At", "Action"], rows);
  el("enrollmentList").innerHTML = "";
  el("enrollmentList").appendChild(table);
}

async function initStudent() {
  if (!getToken()) { window.location.href = "/"; return; }
  if (getRole() !== "STUDENT") { window.location.href = "/teacher.html"; return; }

  el("logoutBtn")?.addEventListener("click", () => { clearAuth(); window.location.href = "/"; });

  try {
    await refreshStudentProfile();
    await refreshCourseCatalog();
    await refreshMyEnrollments();
  } catch (e) {
    showMsg("enrollmentMsg", e.message, false);
  }

  el("studentUpdateForm")?.addEventListener("submit", async (evt) => {
    evt.preventDefault();
    const fd = new FormData(evt.target);
    const payload = Object.fromEntries(fd.entries());
    // empty strings -> null
    for (const k of Object.keys(payload)) {
      if (payload[k] === "") payload[k] = null;
    }

    try {
      await apiFetch("/api/students/me", { method: "PUT", body: JSON.stringify(payload) });
      showMsg("studentUpdateMsg", "Updated.", true);
      await refreshStudentProfile();
    } catch (e) {
      showMsg("studentUpdateMsg", e.message, false);
    }
  });
}

/* -------- teacher.html -------- */
async function refreshTeacherProfile() {
  const t = await apiFetch("/api/teachers/me", { method: "GET" });
  el("teacherProfile").innerHTML = `
    <div><b>${t.fullName}</b> (${t.employeeNo})</div>
    <div>Email: ${t.email}</div>
    <div>Title: ${t.title}</div>
    <div>Department: ${t.department.code} - ${t.department.name}</div>
  `;
}

async function refreshDepartmentsTeacherPage() {
  const depts = await apiFetch("/api/departments", { method: "GET" });

  // populate selects
  await loadDepartmentsInto(el("courseDeptSelect"));
  await loadDepartmentsInto(el("teacherDeptSelect"));

  const rows = depts.map(d => {
    const del = document.createElement("button");
    del.className = "danger";
    del.textContent = "Delete";
    del.addEventListener("click", async () => {
      if (!confirm("Delete department " + d.code + "?")) return;
      try {
        await apiFetch("/api/departments/" + d.id, { method: "DELETE" });
        showMsg("deptMsg", "Deleted " + d.code, true);
        await refreshDepartmentsTeacherPage();
      } catch (e) {
        showMsg("deptMsg", e.message, false);
      }
    });

    return [d.id, d.code, d.name, del];
  });

  const table = renderTable(["ID", "Code", "Name", "Action"], rows);
  el("deptList").innerHTML = "";
  el("deptList").appendChild(table);
}

async function refreshCoursesTeacherPage() {
  const courses = await apiFetch("/api/courses", { method: "GET" });

  const rows = courses.map(c => {
    const del = document.createElement("button");
    del.className = "danger";
    del.textContent = "Delete";
    del.addEventListener("click", async () => {
      if (!confirm("Delete course " + c.code + "?")) return;
      try {
        await apiFetch("/api/courses/" + c.id, { method: "DELETE" });
        showMsg("courseMsg", "Deleted " + c.code, true);
        await refreshCoursesTeacherPage();
      } catch (e) {
        showMsg("courseMsg", e.message, false);
      }
    });

    return [
      c.id,
      c.code,
      c.title,
      c.credit,
      c.department.code,
      `${c.currentlyEnrolled}/${c.capacity}`,
      c.teacher?.fullName || "-",
      del
    ];
  });

  const table = renderTable(["ID", "Code", "Title", "Credit", "Dept", "Enrolled/Cap", "Teacher", "Action"], rows);
  el("teacherCourseList").innerHTML = "";
  el("teacherCourseList").appendChild(table);
}

async function refreshStudentsTeacherPage() {
  const students = await apiFetch("/api/students", { method: "GET" });

  const rows = students.map(s => {
    const disable = document.createElement("button");
    disable.className = "danger";
    disable.textContent = "Disable";
    disable.addEventListener("click", async () => {
      if (!confirm("Disable student " + s.studentNo + " ?")) return;
      try {
        await apiFetch("/api/students/" + s.id, { method: "DELETE" });
        showMsg("studentMsg", "Disabled " + s.studentNo, true);
        await refreshStudentsTeacherPage();
      } catch (e) {
        showMsg("studentMsg", e.message, false);
      }
    });

    const reset = document.createElement("button");
    reset.textContent = "Reset PW";
    reset.addEventListener("click", async () => {
      const newPassword = prompt("Enter new password for " + s.studentNo);
      if (!newPassword) return;
      try {
        await apiFetch("/api/students/" + s.id + "/reset-password", {
          method: "POST",
          body: JSON.stringify({ newPassword })
        });
        showMsg("studentMsg", "Password reset for " + s.studentNo, true);
      } catch (e) {
        showMsg("studentMsg", e.message, false);
      }
    });

    const actions = document.createElement("div");
    actions.className = "inline";
    actions.appendChild(reset);
    actions.appendChild(disable);

    return [s.id, s.studentNo, s.fullName, s.email, s.department.code, s.status, actions];
  });

  const table = renderTable(["ID", "Student No", "Name", "Email", "Dept", "Status", "Actions"], rows);
  el("studentList").innerHTML = "";
  el("studentList").appendChild(table);
}

async function initTeacher() {
  if (!getToken()) { window.location.href = "/"; return; }
  if (getRole() !== "TEACHER") { window.location.href = "/student.html"; return; }

  el("logoutBtn")?.addEventListener("click", () => { clearAuth(); window.location.href = "/"; });

  try {
    await refreshTeacherProfile();
    await refreshDepartmentsTeacherPage();
    await refreshCoursesTeacherPage();
    await refreshStudentsTeacherPage();
  } catch (e) {
    showMsg("studentMsg", e.message, false);
  }

  el("deptCreateForm")?.addEventListener("submit", async (evt) => {
    evt.preventDefault();
    const fd = new FormData(evt.target);
    const payload = Object.fromEntries(fd.entries());
    try {
      await apiFetch("/api/departments", { method: "POST", body: JSON.stringify(payload) });
      showMsg("deptMsg", "Department created.", true);
      evt.target.reset();
      await refreshDepartmentsTeacherPage();
    } catch (e) {
      showMsg("deptMsg", e.message, false);
    }
  });

  el("courseCreateForm")?.addEventListener("submit", async (evt) => {
    evt.preventDefault();
    const fd = new FormData(evt.target);
    const payload = Object.fromEntries(fd.entries());
    payload.credit = Number(payload.credit);
    payload.capacity = Number(payload.capacity);
    payload.departmentId = Number(payload.departmentId);
    if (payload.teacherId === "") delete payload.teacherId;
    else payload.teacherId = Number(payload.teacherId);

    try {
      await apiFetch("/api/courses", { method: "POST", body: JSON.stringify(payload) });
      showMsg("courseMsg", "Course created.", true);
      evt.target.reset();
      await refreshCoursesTeacherPage();
    } catch (e) {
      showMsg("courseMsg", e.message, false);
    }
  });

  el("teacherCreateForm")?.addEventListener("submit", async (evt) => {
    evt.preventDefault();
    const fd = new FormData(evt.target);
    const payload = Object.fromEntries(fd.entries());
    payload.departmentId = Number(payload.departmentId);
    if (payload.hireDate === "") delete payload.hireDate;

    try {
      await apiFetch("/api/teachers", { method: "POST", body: JSON.stringify(payload) });
      showMsg("teacherMsg", "Teacher created.", true);
      evt.target.reset();
    } catch (e) {
      showMsg("teacherMsg", e.message, false);
    }
  });
}

/* -------- router -------- */
document.addEventListener("DOMContentLoaded", () => {
  const path = window.location.pathname;
  if (path === "/" || path.endsWith("/index.html")) initIndex();
  if (path.endsWith("/student.html")) initStudent();
  if (path.endsWith("/teacher.html")) initTeacher();
});
