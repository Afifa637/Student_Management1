const API = "";

function getToken(){ return localStorage.getItem("ue_sms_token"); }
function setToken(t){ localStorage.setItem("ue_sms_token", t); }
function clearAuth(){ localStorage.removeItem("ue_sms_token"); localStorage.removeItem("ue_sms_role"); }
function setRole(r){ localStorage.setItem("ue_sms_role", r); }
function getRole(){ return localStorage.getItem("ue_sms_role"); }

async function apiFetch(path, opts = {}) {
  const headers = opts.headers || {};
  headers["Content-Type"] = "application/json";
  const token = getToken();
  if (token) headers["Authorization"] = "Bearer " + token;

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

const el = (id) => document.getElementById(id);

function showMsg(targetId, message, ok = true) {
  const t = el(targetId);
  if (!t) return;
  t.className = "msg " + (ok ? "ok" : "err");
  t.textContent = message;
}

async function loadDepartmentsInto(selectEl) {
  const depts = await apiFetch("/api/departments", { method: "GET" });
  selectEl.innerHTML = "";
  for (const d of depts) {
    const opt = document.createElement("option");
    opt.value = d.id;
    opt.textContent = `${d.code} - ${d.name}`;
    selectEl.appendChild(opt);
  }
}

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

/* ---------------- Modal helpers ---------------- */
function modalOpen(title, bodyNodeOrHtml) {
  const m = el("modal");
  if (!m) return;
  el("modalTitle").textContent = title;
  const body = el("modalBody");
  body.innerHTML = "";
  if (typeof bodyNodeOrHtml === "string") body.innerHTML = bodyNodeOrHtml;
  else body.appendChild(bodyNodeOrHtml);
  m.classList.remove("hidden");
}

function modalClose() {
  const m = el("modal");
  if (!m) return;
  m.classList.add("hidden");
}

document.addEventListener("click", (e) => {
  const t = e.target;
  if (t?.dataset?.close) modalClose();
});

/* -------- index.html -------- */
async function initIndex() {
  try { await loadDepartmentsInto(el("deptSelect")); }
  catch (e) { showMsg("registerMsg", "Could not load departments: " + e.message, false); }

  el("registerForm")?.addEventListener("submit", async (evt) => {
    evt.preventDefault();
    const fd = new FormData(evt.target);
    const payload = Object.fromEntries(fd.entries());
    payload.departmentId = Number(payload.departmentId);

    try {
      const auth = await apiFetch("/api/auth/register", { method:"POST", body: JSON.stringify(payload) });
      setToken(auth.token); setRole(auth.role);
      showMsg("registerMsg", "Registered successfully. Redirecting...", true);
      window.location.href = "/student.html";
    } catch(e) { showMsg("registerMsg", e.message, false); }
  });

  el("loginForm")?.addEventListener("submit", async (evt) => {
    evt.preventDefault();
    const fd = new FormData(evt.target);
    const payload = Object.fromEntries(fd.entries());
    try {
      const auth = await apiFetch("/api/auth/login", { method:"POST", body: JSON.stringify(payload) });
      setToken(auth.token); setRole(auth.role);
      showMsg("loginMsg", "Login OK. Redirecting...", true);
      window.location.href = auth.role === "TEACHER" ? "/teacher.html" : "/student.html";
    } catch(e) { showMsg("loginMsg", e.message, false); }
  });
}

/* -------- student.html -------- */
async function refreshStudentProfile() {
  const s = await apiFetch("/api/students/me", { method:"GET" });

  // ✅ show all details, including updated contact fields
  el("studentProfile").innerHTML = `
    <div style="display:grid; gap:6px;">
      <div><b style="font-size:16px;">${s.fullName}</b> <span class="muted">(${s.studentNo})</span></div>
      <div><span class="muted">Email:</span> ${s.email}</div>
      <div><span class="muted">Department:</span> ${s.department.code} — ${s.department.name}</div>
      <div><span class="muted">Status:</span> ${s.status}</div>
      <div><span class="muted">Phone:</span> ${s.phone ? s.phone : "-"}</div>
      <div><span class="muted">Address:</span> ${s.address ? s.address : "-"}</div>
    </div>
  `;

  // ✅ prefill form so student sees current values
  const form = el("studentUpdateForm");
  if (form) {
    form.phone.value = s.phone || "";
    form.address.value = s.address || "";
  }
}

async function refreshCourseCatalog() {
  const courses = await apiFetch("/api/courses", { method:"GET" });
  const rows = courses.map(c => {
    const btn = document.createElement("button");
    btn.className = "btn btn-primary";
    btn.textContent = "Enroll";
    btn.addEventListener("click", async () => {
      try {
        await apiFetch("/api/enrollments/me", { method:"POST", body: JSON.stringify({ courseId: c.id }) });
        showMsg("courseMsg", "Enrolled in " + c.code, true);
        await refreshMyEnrollments();
        await refreshCourseCatalog();
      } catch(e) { showMsg("courseMsg", e.message, false); }
    });
    return [c.code, c.title, c.credit, c.department.code, `${c.currentlyEnrolled}/${c.capacity}`, btn];
  });

  const table = renderTable(["Code","Title","Credit","Dept","Enrolled/Cap","Action"], rows);
  el("courseList").innerHTML = "";
  el("courseList").appendChild(table);
}

async function refreshMyEnrollments() {
  const enrollments = await apiFetch("/api/enrollments/me", { method:"GET" });
  const rows = enrollments.map(e => {
    const btn = document.createElement("button");
    btn.className = "btn btn-danger";
    btn.textContent = "Drop";
    btn.disabled = e.status !== "ENROLLED";
    btn.addEventListener("click", async () => {
      try {
        await apiFetch("/api/enrollments/me/" + e.id, { method:"DELETE" });
        showMsg("enrollmentMsg", "Dropped " + e.courseCode, true);
        await refreshMyEnrollments();
        await refreshCourseCatalog();
      } catch(err) { showMsg("enrollmentMsg", err.message, false); }
    });

    return [e.courseCode, e.courseTitle, e.status, e.grade || "-", new Date(e.enrolledAt).toLocaleString(), btn];
  });

  const table = renderTable(["Course","Title","Status","Grade","Enrolled At","Action"], rows);
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
  } catch(e) { showMsg("enrollmentMsg", e.message, false); }

  el("studentUpdateForm")?.addEventListener("submit", async (evt) => {
    evt.preventDefault();
    const fd = new FormData(evt.target);
    const payload = Object.fromEntries(fd.entries());

    // empty string => null (so backend can keep null)
    for (const k of Object.keys(payload)) if (payload[k] === "") payload[k] = null;

    try {
      await apiFetch("/api/students/me", { method:"PUT", body: JSON.stringify(payload) });
      showMsg("studentUpdateMsg", "Contact info updated.", true);

      // ✅ refresh to show updated info immediately
      await refreshStudentProfile();
    } catch(e) { showMsg("studentUpdateMsg", e.message, false); }
  });
}

/* -------- teacher.html -------- */
async function refreshTeacherProfile() {
  const t = await apiFetch("/api/teachers/me", { method:"GET" });
  el("teacherProfile").innerHTML = `
    <div style="display:grid; gap:6px;">
      <div><b style="font-size:16px;">${t.fullName}</b> <span class="muted">(${t.employeeNo})</span></div>
      <div><span class="muted">Email:</span> ${t.email}</div>
      <div><span class="muted">Title:</span> ${t.title}</div>
      <div><span class="muted">Department:</span> ${t.department.code} — ${t.department.name}</div>
    </div>
  `;
}

function btn(label, className, onClick) {
  const b = document.createElement("button");
  b.className = className || "btn btn-ghost";
  b.textContent = label;
  b.addEventListener("click", onClick);
  return b;
}

/* Departments: add Update */
async function refreshDepartmentsTeacherPage() {
  const depts = await apiFetch("/api/departments", { method:"GET" });

  await loadDepartmentsInto(el("courseDeptSelect"));
  await loadDepartmentsInto(el("teacherDeptSelect"));

  const rows = depts.map(d => {
    const actions = document.createElement("div");
    actions.className = "inline";

    const edit = btn("Update", "btn btn-ghost", () => openDeptEdit(d));
    const del = btn("Delete", "btn btn-danger", async () => {
      if (!confirm(`Delete department ${d.code}?`)) return;
      try {
        await apiFetch("/api/departments/" + d.id, { method:"DELETE" });
        showMsg("deptMsg", `Deleted ${d.code}`, true);
        await refreshDepartmentsTeacherPage();
      } catch(e) { showMsg("deptMsg", e.message, false); }
    });

    actions.appendChild(edit);
    actions.appendChild(del);
    return [d.id, d.code, d.name, actions];
  });

  const table = renderTable(["ID","Code","Name","Actions"], rows);
  el("deptList").innerHTML = "";
  el("deptList").appendChild(table);
}

function openDeptEdit(d) {
  const form = document.createElement("form");
  form.className = "form";
  form.innerHTML = `
    <div class="form-row">
      <label>Code <input name="code" maxlength="20" required/></label>
      <label>Name <input name="name" maxlength="120" required/></label>
    </div>
    <div class="form-actions">
      <button class="btn btn-primary" type="submit">Save</button>
      <span id="deptEditMsg" class="msg"></span>
    </div>
  `;
  form.code.value = d.code;
  form.name.value = d.name;

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = { code: form.code.value, name: form.name.value };
    try {
      await apiFetch("/api/departments/" + d.id, { method:"PUT", body: JSON.stringify(payload) });
      showMsg("deptEditMsg", "Updated successfully.", true);
      await refreshDepartmentsTeacherPage();
      modalClose();
    } catch(err) { showMsg("deptEditMsg", err.message, false); }
  });

  modalOpen(`Update Department: ${d.code}`, form);
}

/* Courses: add Update */
async function refreshCoursesTeacherPage() {
  const courses = await apiFetch("/api/courses", { method:"GET" });

  const rows = courses.map(c => {
    const actions = document.createElement("div");
    actions.className = "inline";

    const edit = btn("Update", "btn btn-ghost", () => openCourseEdit(c));
    const del = btn("Delete", "btn btn-danger", async () => {
      if (!confirm(`Delete course ${c.code}?`)) return;
      try {
        await apiFetch("/api/courses/" + c.id, { method:"DELETE" });
        showMsg("courseMsg", `Deleted ${c.code}`, true);
        await refreshCoursesTeacherPage();
      } catch(e) { showMsg("courseMsg", e.message, false); }
    });

    actions.appendChild(edit);
    actions.appendChild(del);

    return [
      c.id, c.code, c.title, c.credit, c.department.code,
      `${c.currentlyEnrolled}/${c.capacity}`,
      c.teacher?.fullName || "-",
      actions
    ];
  });

  const table = renderTable(["ID","Code","Title","Credit","Dept","Enrolled/Cap","Teacher","Actions"], rows);
  el("teacherCourseList").innerHTML = "";
  el("teacherCourseList").appendChild(table);
}

async function openCourseEdit(c) {
  const form = document.createElement("form");
  form.className = "form";
  form.innerHTML = `
    <div class="form-row">
      <label>Code <input name="code" maxlength="30" required/></label>
      <label>Title <input name="title" maxlength="160" required/></label>
    </div>
    <div class="form-row">
      <label>Credit <input name="credit" type="number" step="0.5" min="0.5" required/></label>
      <label>Capacity <input name="capacity" type="number" min="1" required/></label>
    </div>
    <div class="form-row">
      <label>Department
        <select name="departmentId" id="courseEditDeptSelect" required></select>
      </label>
      <label>TeacherId (optional)
        <input name="teacherId" type="number" placeholder="Empty = keep current"/>
      </label>
    </div>
    <div class="form-actions">
      <button class="btn btn-primary" type="submit">Save</button>
      <span id="courseEditMsg" class="msg"></span>
    </div>
  `;

  // load depts into modal select
  await loadDepartmentsInto(form.querySelector("#courseEditDeptSelect"));

  form.code.value = c.code;
  form.title.value = c.title;
  form.credit.value = c.credit;
  form.capacity.value = c.capacity;
  form.departmentId.value = String(c.department.id);

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = {
      code: form.code.value,
      title: form.title.value,
      credit: Number(form.credit.value),
      capacity: Number(form.capacity.value),
      departmentId: Number(form.departmentId.value)
    };

    const tid = form.teacherId.value.trim();
    if (tid !== "") payload.teacherId = Number(tid);

    try {
      await apiFetch("/api/courses/" + c.id, { method:"PUT", body: JSON.stringify(payload) });
      showMsg("courseEditMsg", "Updated successfully.", true);
      await refreshCoursesTeacherPage();
      modalClose();
    } catch(err) { showMsg("courseEditMsg", err.message, false); }
  });

  modalOpen(`Update Course: ${c.code}`, form);
}

/* Students list */
async function refreshStudentsTeacherPage() {
  const students = await apiFetch("/api/students", { method:"GET" });

  const rows = students.map(s => {
    const actions = document.createElement("div");
    actions.className = "inline";

    const reset = btn("Reset PW", "btn btn-ghost", async () => {
      const newPassword = prompt("Enter new password for " + s.studentNo);
      if (!newPassword) return;
      try {
        await apiFetch(`/api/students/${s.id}/reset-password`, {
          method:"POST",
          body: JSON.stringify({ newPassword })
        });
        showMsg("studentMsg", "Password reset for " + s.studentNo, true);
      } catch(e) { showMsg("studentMsg", e.message, false); }
    });

    const disable = btn("Disable", "btn btn-danger", async () => {
      if (!confirm("Disable student " + s.studentNo + " ?")) return;
      try {
        await apiFetch("/api/students/" + s.id, { method:"DELETE" });
        showMsg("studentMsg", "Disabled " + s.studentNo, true);
        await refreshStudentsTeacherPage();
      } catch(e) { showMsg("studentMsg", e.message, false); }
    });

    actions.appendChild(reset);
    actions.appendChild(disable);

    return [s.id, s.studentNo, s.fullName, s.email, s.department.code, s.status, actions];
  });

  const table = renderTable(["ID","Student No","Name","Email","Dept","Status","Actions"], rows);
  el("studentList").innerHTML = "";
  el("studentList").appendChild(table);
}

/* Teachers list (new) */
async function refreshTeachersTeacherPage() {
  const teachers = await apiFetch("/api/teachers", { method:"GET" });
  const me = await apiFetch("/api/teachers/me", { method:"GET" });

  const rows = teachers.map(t => {
    const actions = document.createElement("div");
    actions.className = "inline";

    const reset = btn("Reset PW", "btn btn-ghost", async () => {
      const newPassword = prompt("Enter new password for " + t.employeeNo);
      if (!newPassword) return;
      try {
        await apiFetch(`/api/teachers/${t.id}/reset-password`, {
          method:"POST",
          body: JSON.stringify({ newPassword })
        });
        showMsg("teacherListMsg", "Password reset for " + t.employeeNo, true);
      } catch(e) { showMsg("teacherListMsg", e.message, false); }
    });

    const disable = btn("Disable", "btn btn-danger", async () => {
      if (t.id === me.id) {
        showMsg("teacherListMsg", "You cannot disable your own account.", false);
        return;
      }
      if (!confirm("Disable teacher " + t.employeeNo + " ?")) return;
      try {
        await apiFetch("/api/teachers/" + t.id, { method:"DELETE" });
        showMsg("teacherListMsg", "Disabled " + t.employeeNo, true);
        await refreshTeachersTeacherPage();
      } catch(e) { showMsg("teacherListMsg", e.message, false); }
    });

    actions.appendChild(reset);
    actions.appendChild(disable);

    return [t.id, t.employeeNo, t.fullName, t.email, t.title, t.department.code, actions];
  });

  const table = renderTable(["ID","Emp No","Name","Email","Title","Dept","Actions"], rows);
  el("teacherList").innerHTML = "";
  el("teacherList").appendChild(table);
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
    await refreshTeachersTeacherPage();
  } catch(e) { showMsg("studentMsg", e.message, false); }

  el("deptCreateForm")?.addEventListener("submit", async (evt) => {
    evt.preventDefault();
    const fd = new FormData(evt.target);
    const payload = Object.fromEntries(fd.entries());
    try {
      await apiFetch("/api/departments", { method:"POST", body: JSON.stringify(payload) });
      showMsg("deptMsg", "Department created.", true);
      evt.target.reset();
      await refreshDepartmentsTeacherPage();
    } catch(e) { showMsg("deptMsg", e.message, false); }
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
      await apiFetch("/api/courses", { method:"POST", body: JSON.stringify(payload) });
      showMsg("courseMsg", "Course created.", true);
      evt.target.reset();
      await refreshCoursesTeacherPage();
    } catch(e) { showMsg("courseMsg", e.message, false); }
  });

  el("teacherCreateForm")?.addEventListener("submit", async (evt) => {
    evt.preventDefault();
    const fd = new FormData(evt.target);
    const payload = Object.fromEntries(fd.entries());
    payload.departmentId = Number(payload.departmentId);
    if (payload.hireDate === "") delete payload.hireDate;

    try {
      await apiFetch("/api/teachers", { method:"POST", body: JSON.stringify(payload) });
      showMsg("teacherMsg", "Teacher created.", true);
      evt.target.reset();
      await refreshTeachersTeacherPage();
    } catch(e) { showMsg("teacherMsg", e.message, false); }
  });
}

/* -------- router -------- */
document.addEventListener("DOMContentLoaded", () => {
  const path = window.location.pathname;
  if (path === "/" || path.endsWith("/index.html")) initIndex();
  if (path.endsWith("/student.html")) initStudent();
  if (path.endsWith("/teacher.html")) initTeacher();
});
