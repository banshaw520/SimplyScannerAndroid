好的，這份文件是基於您提供的 `MainPro.storyboard` 檔案所產生的畫面需求規格。

---

### **畫面需求文件 - SimplyScanner Pro**

此文件描述了 `MainPro.storyboard` 中定義的主要畫面的佈局、圖片資源、文字內容及互動流程。

### **畫面一：文件列表 (DocsViewController)**

這是應用程式的主要畫面，用於顯示、管理文件和資料夾。

#### **1. 佈局 (Layout)**

*   **導覽列 (Navigation Bar)**:
    *   **左側按鈕**:
        *   返回按鈕 (`keyboard-backspace`)
        *   設定按鈕 (`gearshape`)
    *   **標題**: "Documents" (可點擊)
    *   **右側按鈕**: "Cancel" (在特定模式下顯示，例如選擇模式)
*   **工具列 (Toolbar)**: 位於導覽列下方，包含一排水平排列的功能按鈕。
    *   搜尋 (`magnifyingglass`)
    *   從相簿加入 (`photo`)
    *   建立資料夾 (`folder-plus-outline-normal`)
    *   排序 (`sort-alphabetical-ascending`)
    *   切換檢視樣式 (`square.grid.3x3`)
    *   進入選擇模式 (`checkbox-marked-circle-normal`)
*   **搜尋列 (Search Bar)**: 預設隱藏，點擊搜尋按鈕後展開。
*   **內容區 (Content Area)**:
    *   **文件/資料夾網格 (Collection View)**: 主要區域，用以網格或列表形式顯示文件和資料夾。
    *   **空狀態視圖 (Empty State View)**: 當沒有任何文件時顯示。
        *   包含一個預設圖示 (`notes-150587`)。
        *   提示文字："Add your first document!"。
        *   一個指向下方相機按鈕的箭頭圖示 (`arrow-down-bold`)。
    *   **無搜尋結果提示 (No Result Label)**: 當搜尋沒有結果時，顯示 "No Result" 文字。
*   **底部指令列 (Bottom Command Bar)**:
    *   **相機按鈕**: 一個置中、突出的圓形按鈕，用於啟動相機掃描。
*   **多選指令列 (Selection Command View)**: 當進入選擇模式時，取代底部相機按鈕，提供針對多選項目的操作。
    *   刪除 (`trash.fill`)
    *   複製 (`content-save-all-outline-normal`)
    *   移動 (`content-save-move-outline-normal`)
    *   分享 (`square.and.arrow.up`)

#### **2. 圖片名稱 (Picture Name)**

| 元件 | 圖片名稱 | 備註 |
| :--- | :--- | :--- |
| 返回按鈕 | `keyboard-backspace` | |
| 設定按鈕 | `gearshape` | 系統圖示 |
| 搜尋按鈕 | `magnifyingglass` | 系統圖示 |
| 從相簿加入按鈕 | `photo` | 系統圖示 |
| 建立資料夾按鈕 | `folder-plus-outline-normal` | |
| 排序按鈕 | `sort-alphabetical-ascending` | |
| 檢視樣式按鈕 | `square.grid.3x3` | 系統圖示，使用包含 Alpha 色板的階層式顏色 (Hierarchical Colors) |
| 選擇項目按鈕 | `checkbox-marked-circle-normal` | |
| 空狀態圖示 | `notes-150587` | |
| 空狀態箭頭 | `arrow-down-bold` | |
| 相機按鈕 | `camera` | |
| 刪除按鈕 (多選) | `trash.fill` | 系統圖示，高亮狀態使用包含 Alpha 色板的階層式顏色 |
| 複製按鈕 (多選) | `content-save-all-outline-normal` | |
| 移動按鈕 (多選) | `content-save-move-outline-normal` | |
| 分享按鈕 (多選) | `square.and.arrow.up` | 系統圖示，高亮狀態使用包含 Alpha 色板的階層式顏色 |

#### **3. 字串與顏色 (String & Color)**

| 元件 | 文字內容 | 字型 | 大小 | 顏色 (Foreground) | 背景顏色 (Background) | Alpha |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 導覽列標題 | "Documents" | Helvetica Neue | 20 | `darkSlateBlueThree` | - | 1.0 |
| 導覽列按鈕 | "Cancel" / 圖示 | (系統預設) | (系統預設) | `darkSlateBlueThree` | - | 1.0 |
| 工具列按鈕 | (圖示) | - | - | `slateGrey` | `white` | 1.0 |
| 空狀態提示 | "Add your first document!" | Helvetica Neue | 15 | `blueGrey` | - | 1.0 |
| 無搜尋結果提示 | "No Result" | Helvetica Neue | 17 | `blueGrey` | `clear` | 1.0 (文字), 0.0 (背景) |
| 多選指令列文字 | "Delete", "Copy", "Move", "Share" | Helvetica Neue | 11 | `steel` | - | 1.0 |
| 多選指令列圖示 | (圖示) | - | - | `darkSlateBlueThree` | - | 1.0 |
| 內容區背景 | - | - | - | - | `Gainsboro` | 1.0 |
| 相機按鈕 | (圖示) | - | - | `blueberry` | `babyBlue` | 1.0 |

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

*   **內容區 (Content Area)**:
    *   **點擊文件**: 導航至 **畫面二 (ScanDocViewController)**，顯示該文件的詳細內容。
    *   **點擊資料夾**: 進入該資料夾，更新導覽列標題為資料夾名稱，並顯示資料夾內的內容。
    *   **長按項目**: 進入「選擇模式」，並自動選取被長按的項目。

*   **底部指令列 (Bottom Command Bar)**:
    *   **相機按鈕**: 導航至相機掃描介面，開始新的掃描流程。

*   **多選指令列 (Selection Command View)**:
    *   **刪除按鈕**: 彈出確認對話框，確認後刪除所有被選取的項目。
    *   **複製按鈕**: 複製所選項目。
    *   **移動按鈕**: 開啟資料夾選擇器，讓使用者選擇目標資料夾以移動所選項目。
    *   **分享按鈕**: 彈出系統的分享選單，提供分享所選項目(匯出為 PDF、JPG 等)的選項。

---

### **畫面二：掃描文件檢視 (ScanDocViewController)**

此畫面用於檢視、編輯和管理單一份額掃描文件中的頁面。

#### **1. 佈局 (Layout)**

*   **導覽列 (Navigation Bar)**:
    *   **左側按鈕**: 返回按鈕 (`keyboard-backspace`)
    *   **標題**: 動態顯示當前文件名稱。
*   **主要內容區 (Main Content Area)**:
    *   **頁面輪播 (Carousel View)**: 佔據大部分畫面，可水平滑動切換單張頁面。
    *   **頁碼指示器**: 位於輪播圖下方，格式為 "目前頁數 / 總頁數" (例如 "1 / 5")。
*   **縮圖預覽列 (Thumbnails View)**: 位於頁面輪播下方，水平滾動顯示所有頁面的縮圖。
*   **底部指令列 (Bottom Command Bar)**:
    *   **新增頁面按鈕**: 位於左側。
    *   **刪除頁面按鈕**: 位於左二。
    *   **分享文件按鈕**: 置中、突出的圓形按鈕。
    *   **編輯頁面按鈕**: 位於右二。
    *   **重拍頁面按鈕**: 位於右側。

#### **2. 圖片名稱 (Picture Name)**

| 元件 | 圖片名稱 | 備註 |
| :--- | :--- | :--- |
| 返回按鈕 | `keyboard-backspace` | |
| 新增頁面按鈕 | `file-image-plus-outline-normal` | |
| 刪除頁面按鈕 | `file-image-remove-outline-normal` | |
| 分享文件按鈕 | `square.and.arrow.up` | 系統圖示，使用包含 Alpha 色板的階層式顏色 |
| 編輯頁面按鈕 | `image-edit-outline-normal` | |
| 重拍頁面按鈕 | `camera-retake-outline-normal` | |

#### **3. 字串與顏色 (String & Color)**

| 元件 | 文字內容 | 字型 | 大小 | 顏色 (Foreground) | 背景顏色 (Background) | Alpha |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 導覽列標題 | (動態文件名稱) | Helvetica Neue | 20 | `darkSlateBlueThree` | - | 1.0 |
| 導覽列返回按鈕 | (圖示) | - | - | `darkSlateBlueThree` | - | 1.0 |
| 頁碼指示器 | "1 / n" | Helvetica Neue | 14 | `steel` | - | 1.0 |
| 縮圖頁碼 | (頁碼) | Helvetica Neue | 10 | `steel` | - | 1.0 |
| 底部指令列文字 | "Add Page", "Del Page", "Edit", "Retake" | Helvetica Neue | 11 | `steel` | - | 1.0 |
| 底部指令列圖示 | (圖示) | - | - | `slateGrey` | - | 1.0 |
| 分享按鈕 | (圖示) | - | - | `darkSlateBlueThree` | `babyBlue` | 1.0 |
| 內容區背景 | - | - | - | - | `Gainsboro` | 1.0 |

#### **4. 使用者互動流程 (User Interactions)**

*   **導覽列 (Navigation Bar)**:
    *   **返回按鈕 (`keyboard-backspace`)**: 返回 **畫面一 (DocsViewController)**。
    *   **標題按鈕 (文件名稱)**: 彈出對話框，允許使用者重新命名目前的文件。

*   **主要內容區 (Main Content Area)**:
    *   **頁面輪播 (Carousel View)**:
        *   **左右滑動**: 切換顯示不同的頁面。滑動結束後，下方的縮圖預覽列會同步捲動到對應的縮圖位置。
        *   **雙指縮放**: 放大或縮小當前顯示的頁面。

*   **縮圖預覽列 (Thumbnails View)**:
    *   **點擊縮圖**: 上方的頁面輪播會跳轉至對應的頁面。
    *   **長按並拖曳縮圖**: 允許使用者重新排序頁面。

*   **底部指令列 (Bottom Command Bar)**:
    *   **新增頁面按鈕**: 彈出選項，讓使用者選擇從「相機」或「相簿」新增頁面。
    *   **刪除頁面按鈕**: 刪除目前在輪播圖中顯示的頁面。
    *   **分享文件按鈕**: 彈出系統的分享選單，提供分享整個文件(匯出為 PDF、JPG 等)的選項。
    *   **編輯頁面按鈕**: 導航至圖片編輯畫面 (例如：ZLImageEditor)，對當前頁面進行裁切、旋轉、濾鏡等操作。
    *   **重拍頁面按鈕**: 導航至相機介面，讓使用者重新拍攝當前頁面，並以新拍攝的影像取代。

---