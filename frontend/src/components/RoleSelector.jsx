function RoleSelector({ activeRole, onChange, allowedRoles }) {
  return (
    <div className="role-strip">
      {allowedRoles.map((role) => (
        <button
          key={role}
          type="button"
          className={role === activeRole ? "role-chip active" : "role-chip"}
          onClick={() => onChange(role)}
        >
          {role.toLowerCase()}
        </button>
      ))}
    </div>
  );
}

export default RoleSelector;
