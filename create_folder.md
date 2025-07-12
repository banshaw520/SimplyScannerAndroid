### **產品需求文件 (PRD)：建立新資料夾 (Android 遷移)**

**1. 功能概述**

*   **目標**: 將 iOS 應用程式中的「建立新資料夾」功能完整遷移至 Android 平台。此功能允許使用者在文件列表中建立新的資料夾，以組織和管理掃描文件。
*   **核心原則**: Android 版本的實現應在後端邏輯、檔案系統結構和使用者體驗上，與 `ScanService.swift` 中定義的 iOS 版本保持功能對等 (functional parity)。

**2. 相關 iOS 程式碼參考**

*   **主要邏輯**: `SimplyScanner/ProVersion/Service/ScanService.swift`
*   **核心函式**:
    *   `createFolder(path: String, displayName: String)`
    *   `private create(path: String, displayName: String, bFolder: Bool)`
*   **資料模型**: `ScanItem`

**3. 使用者流程與 UI 設計**

1.  **觸發點**:
    *   在主文件列表畫面 (Documents) 的工具列上，使用者點擊「建立資料夾」圖示按鈕。

2.  **互動對話框 (Dialog)**:
    *   點擊後，系統應彈出一個對話框 (在 Android 中可使用 `AlertDialog` 搭配自訂 View)。
    *   **標題**: "Create new folder"。
    *   **輸入框 (EditText)**:
        *   應自動獲取焦點並顯示鍵盤。
        *   包含預設文字，例如 "New Folder" (詳見 FR-2)。
    *   **按鈕**:
        *   **"Cancel"**: 點擊後，關閉對話框，取消操作。
        *   **"Ok"**: 點擊後，觸發資料夾建立的驗證與後端邏輯。

3.  **成功流程**:
    *   點擊 "Ok" 且驗證通過後，對話框關閉。
    *   新的資料夾項目應立即出現在文件列表中，並遵循當前的排序規則。
    *   畫面應平滑滾動以顯示新建立的資料夾。

4.  **失敗流程**:
    *   若名稱驗證失敗 (例如，名稱重複)，應顯示一個提示訊息 (例如 `Toast` 或 `Snackbar`)，且對話框不應關閉，以便使用者修改。

**4. 功能規格 (Functional Requirements)**

*   **FR-1: 資料夾建立與檔案系統結構 (參照 `create` 函式)**
    *   **FR-1.1**: 建立資料夾並**不**是直接在檔案系統上建立一個以使用者輸入名稱命名的目錄。
    *   **FR-1.2**: 系統應產生一個基於**高精度時間戳 (timestamp)** 的唯一目錄名稱，格式為 `yyyy-MM-dd_HH_mm_ss.SSS`。
    *   **FR-1.3**: 在這個以時間戳命名的目錄內部，必須建立一個名為 `desc.ini` 的元數據 (metadata) 檔案。
    *   **FR-1.4**: 使用者輸入的顯示名稱 (`displayName`)、該項目是資料夾的標記 (`bDir = true`)，以及其他如 `uuid`、`createdDate` 等 `ScanItem` 屬性，都將被寫入 `desc.ini` 檔案中。
    *   **FR-1.5**: 應用程式在讀取和顯示文件列表時，是透過掃描這些時間戳目錄並解析其內部的 `desc.ini` 檔案來獲取 `displayName` 的。

*   **FR-2: 預設資料夾名稱 **
    *   **FR-2.1**: 當對話框出現時，系統應提供一個預設名稱。
    *   **FR-2.2**: 檢查當前目錄下是否存在 `displayName` 為 "New Folder" 的項目。
        *   如果不存在，預設名稱為 "New Folder"。
        *   如果已存在，則檢查 "New Folder (1)"。
        *   如果 "New Folder (1)" 也存在，則檢查 "New Folder (2)"，依此類推，直到找到一個不重複的名稱。

*   **FR-3: 資料夾名稱驗證**
    *   **FR-3.1**: 名稱不能為空。當輸入框為空時，"Ok" 按鈕應呈現禁用 (disabled) 狀態。
    *   **FR-3.2**: 名稱在同一個父目錄下必須是唯一的 (`displayName` 不得重複)。
    *   **FR-3.3**: 在儲存前，應自動去除使用者輸入名稱的頭尾空白字元。

*   **FR-4: 資料持久化與模型 (對應 `ScanItem`)**
    *   **FR-4.1**: 成功建立後，必須將包含以下屬性的 `ScanItem` 物件序列化並儲存至 `desc.ini`：
        *   `uuid`: `String` (一個新的 UUID)
        *   `displayName`: `String` (使用者輸入的名稱)
        *   `createdDate`: `Date`
        *   `updatedDate`: `Date`
        *   `bDir`: `Boolean` (應設為 `true`)
        *   `relativePath`: `String` 
        *   其他相關屬性 (如 `bLock`, `deletedDate` 等應設為預設值)
    *   **FR-4.2: `relativePath` 的生成邏輯 (巢狀結構)**
        *   `relativePath` 是一個**累積路徑**，代表項目在檔案系統中的實際巢狀位置。
        *   其生成規則為：**父資料夾的 `relativePath` + 當前項目自己的時間戳目錄名稱 + "/"**。
        *   **範例**:
            1.  在根目錄 (`./`) 下建立 `folder1` (時間戳目錄為 `ts1`)，其 `relativePath` 為 `./ts1/`。
            2.  在 `folder1` 內建立 `folder2` (時間戳目錄為 `ts2`)，其 `relativePath` 會是 `./ts1/ts2/`。
            3.  在 `folder2` 內建立 `folder3` (時間戳目錄為 `ts3`)，其 `relativePath` 則為 `./ts1/ts2/ts3/`。

*   **FR-5: UI 更新**
    *   **FR-5.1**: 建立資料夾後，驅動文件列表的 `Adapter` (例如 `RecyclerView.Adapter`) 必須被通知資料已變更，並立即刷新 UI。

**5. 非功能性需求 (Non-Functional Requirements)**

*   **NFR-1: 錯誤處理 (Error Handling)**
    *   應處理檔案系統操作可能發生的錯誤 (例如，權限不足、磁碟空間已滿)，並向使用者顯示友善的錯誤提示。