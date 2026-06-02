# DarkNote

## Memory System

This project uses the universal memory system. **Always follow this workflow:**

### Session Start
```bash
cd /home/dark/Project/darknote
memory status        # get context, pending TODOs, last session
```

### During Work
```bash
memory add-decision "decision text" "why"    # technical choices
memory add-learning "lesson" "context"       # discoveries
memory add-todo "task" high                  # future work
```

### Session End (REQUIRED — do not skip)
```bash
memory complete-todo "task text"             # mark done tasks
memory end-session "summary of what was done"
```

### Rules
- ❌ NEVER use Python/SQL directly with memory system
- ✅ ALWAYS cd to project dir first (project detection is cwd-based)
- ✅ ALWAYS run `end-session` before finishing work
