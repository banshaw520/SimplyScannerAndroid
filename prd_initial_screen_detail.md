#### **1. 佈局 (Layout)**

*   **導覽列 (Navigation Bar)**:
    *   **左側按鈕**:
        *   返回按鈕 (`keyboard_backspace`)
        *   設定按鈕 (`gearshape`)
    *   **標題**: "Documents" (可點擊)
    *   **右側按鈕**: "Cancel" (在特定模式下顯示，例如選擇模式)
*   **工具列 (Toolbar)**: 位於導覽列下方，包含一排水平排列的功能按鈕。
    *   搜尋 (`magnifyingglass`)
    *   從相簿加入 (`photo`)
    *   建立資料夾 (`folder_plus_outline_normal`)
    *   排序 (`sort_alphabetical_ascending`)
    *   切換檢視樣式 (`square.grid.3x3`)
    *   進入選擇模式 (`checkbox_marked_circle_normal`)
*   **搜尋列 (Search Bar)**: 預設隱藏，點擊搜尋按鈕後展開。
*   **內容區 (Content Area)**:
    *   **文件/資料夾網格 (Collection View)**: 主要區域，用以網格或列表形式顯示文件和資料夾。
    *   **空狀態視圖 (Empty State View)**: 當沒有任何文件時顯示。
        *   包含一個預設圖示 (`notes_150587`)。
        *   提示文字："Add your first document!"。
        *   一個指向下方相機按鈕的箭頭圖示 (`arrow_down_bold`)。
    *   **無搜尋結果提示 (No Result Label)**: 當搜尋沒有結果時，顯示 "No Result" 文字。
*   **底部指令列 (Bottom Command Bar)**:
    *   **相機按鈕**: 一個置中、突出的圓形按鈕，用於啟動相機掃描。

#### **2. 圖片名稱 (Picture Name)**

| 元件 | 圖片名稱 | 備註 |
| :--- | :--- | :--- |
| 返回按鈕 | `keyboard_backspace` | |
| 設定按鈕 | `gearshape` | 系統圖示 |
| 搜尋按鈕 | `magnifyingglass` | 系統圖示 |
| 從相簿加入按鈕 | `photo` | 系統圖示 |
| 建立資料夾按鈕 | `folder_plus_outline_normal` | |
| 排序按鈕 | `sort-alphabetical_ascending` | |
| 檢視樣式按鈕 | `square.grid.3x3` | 系統圖示，使用包含 Alpha 色板的階層式顏色 (Hierarchical Colors) |
| 選擇項目按鈕 | `checkbox_marked_circle_normal` | |
| 空狀態圖示 | `notes_150587` | |
| 空狀態箭頭 | `arrow_down_bold` | |
| 相機按鈕 | `camera` | |


#### **3. 字串 (String)**

| 元件 | 文字內容 | 字型 | 大小 |
| :--- | :--- | :--- | :--- |
| 導覽列標題 | "Documents" | Helvetica Neue | 20 |
| 導覽列右按鈕 | "Cancel" | (系統預設) | (系統預設) |
| 空狀態提示 | "Add your first document!" | Helvetica Neue | 15 |
| 無搜尋結果提示 | "No Result" | Helvetica Neue | 17 |

| 元件 | 文字內容 | 字型 | 大小 | 顏色 (Foreground) | 背景顏色 (Background) | Alpha |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 導覽列標題 | "Documents" | Helvetica Neue | 20 | `darkSlateBlueThree` | - | 1.0 |
| 導覽列按鈕 | "Cancel" / 圖示 | (系統預設) | (系統預設) | `darkSlateBlueThree` | - | 1.0 |
| 工具列按鈕 | (圖示) | - | - | `slateGrey` | `white` | 1.0 |
| 空狀態提示 | "Add your first document!" | Helvetica Neue | 15 | `blueGrey` | - | 1.0 |
| 無搜尋結果提示 | "No Result" | Helvetica Neue | 17 | `blueGrey` | `clear` | 1.0 (文字), 0.0 (背景) |
| 內容區背景 | - | - | - | - | `Gainsboro` | 1.0 |
| 相機按鈕 | (圖示) | - | - | `blueberry` | `babyBlue` | 1.0 |

---

#### **4. 使用者互動流程 (User Interactions)**

*   **導覽列 (Navigation Bar)**:
    *   **返回按鈕 (`keyboard-backspace`)**: 若在資料夾內，則返回上一層。若在根目錄，則可能無作用或關閉某個模式。
    *   **設定按鈕 (`gearshape`)**: 導航至應用程式的設定畫面。
    *   **標題按鈕 ("Documents")**: 點擊後可能觸發返回根目錄的功能。
    *   **"Cancel" 按鈕**: 退出當前的特殊模式 (例如：選擇模式)，並隱藏多選指令列。

*   **工具列 (Toolbar)**:
    *   **搜尋按鈕 (`magnifyingglass`)**: 隱藏工具列，並顯示搜尋列 (`SearchBar`)，讓使用者可以輸入關鍵字。
    *   **從相簿加入按鈕 (`photo`)**: 開啟系統的圖片選擇器，讓使用者可以從相簿匯入圖片來建立新文件。
    *   **建立資料夾按鈕 (`folder-plus-outline-normal`)**: 彈出對話框，讓使用者輸入新資料夾的名稱並建立。
    *   **排序按鈕 (`sort-alphabetical-ascending`)**: 彈出排序選項 (例如：依名稱、日期)，點選後重新排列內容區的項目。
    *   **檢視樣式按鈕 (`square.grid.3x3`)**: 切換內容區的顯示方式，例如在網格檢視和列表檢視之間切換。
    *   **選擇項目按鈕 (`checkbox-marked-circle-normal`)**: 進入「選擇模式」，隱藏工具列和底部相機按鈕，顯示多選指令列，並在每個文件/資料夾上顯示核取方塊。

*   **底部指令列 (Bottom Command Bar)**:
    *   **相機按鈕**: 導航至相機掃描介面，開始新的掃描流程。

---
