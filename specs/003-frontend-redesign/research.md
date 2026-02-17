# Research: Frontend Redesign - Multi-tab Version Selection

## Technical Context Research

### Frameworks & Libraries
- **Backend**: Quarkus 3.x (Java 21)
- **Templating**: Quarkus Qute (server-side rendering)
- **Frontend CSS**: Custom `theme.css` using CSS Variables
- **Frontend JS**: Vanilla JavaScript (ES6+)
- **Icons**: Emoji/Unicode (matching existing style)

### UI Components Research
- **Tabs**: Will implement using radio-button or button-based toggle with `display: none/block` on target containers. Horizontal Pill style chosen.
- **Searchable Combobox**: 
  - Need to handle ~500 items from `java_versions.txt`.
  - Native `<datalist>` is an option but styling is limited.
  - Custom implementation: `<input type="text">` + filtered `<ul>` or `<select>` with search.
  - *Decision*: Simple custom implementation with `input` and filtered `div/ul` for better UX and consistency with `theme.css`.
- **Drag & Drop**: Already partially implemented for file uploads. Will extend to text area for `.txt` files.

### Backend Contract (Deferred but planned)
- **Quick Selection**: `POST /analyze/quick` with `version` parameter.
- **List of Versions**: `POST /analyze/list` with `versions` (text) parameter.
- **Detailed Audit**: `POST /upload` with `file` and `email` parameters (existing endpoint to be adapted).

## Design Decisions

- **DD-001: Component Isolation**: Each tab's content will be wrapped in a separate `<section>` or `<div>`.
- **DD-002: State Management**: Tab state managed by a simple URL hash (e.g., `#quick`, `#list`, `#detailed`) or just local JS state to avoid full page reloads.
- **DD-003: Version Loading**: `java_versions.txt` will be loaded into the page context (via Qute or a separate fetch) to populate the combobox.
- **DD-004: Email Validation**: Simple regex check on frontend before submission in the Detailed Audit tab.

## Implementation Path

1. **Phase 1**: Structure the new 3-tab layout in `index.html`.
2. **Phase 2**: Move existing "Detailed Audit" content and "Step-by-Step Guide" into the 3rd tab.
3. **Phase 3**: Implement "Quick Selection" tab with the searchable combobox.
4. **Phase 4**: Implement "List of Versions" tab with text area and drag-and-drop support.
5. **Phase 5**: Add JS logic for tab switching and searchable filtering.
