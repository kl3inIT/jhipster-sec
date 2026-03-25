---
status: complete
phase: 08-user-management-delivery
source:
  - 08-01-SUMMARY.md
  - 08-02-SUMMARY.md
  - 08-03-SUMMARY.md
  - 08-04-SUMMARY.md
started: 2026-03-26T00:26:19.9328153+07:00
updated: 2026-03-26T00:31:40.0000000+07:00
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

[testing complete]

## Tests

### 1. Mở danh sách người dùng và tìm kiếm
expected: Mở `/admin/users`. Bạn sẽ thấy bảng danh sách tài khoản với phân trang và các cột login, họ tên, email, trạng thái, vai trò, lần sửa cuối và thao tác. Khi nhập một phần login, email hoặc tên vào ô tìm kiếm, danh sách phải lọc đúng mà không vỡ phân trang hoặc điều hướng.
result: pass

### 2. Xem chi tiết một người dùng
expected: Từ danh sách, mở một người dùng tại `/admin/users/:login/view`. Màn hình chi tiết phải hiển thị thẻ thông tin người dùng ở bên trái, thẻ vai trò ở bên phải, và nút quay lại phải đưa bạn về đúng ngữ cảnh danh sách trước đó.
result: pass

### 3. Tạo mới người dùng và gán vai trò
expected: Mở `/admin/users/new`. Màn hình phải là form 2 cột với thông tin tài khoản bên trái và vai trò bên phải. Nhập dữ liệu hợp lệ, chọn vai trò, lưu thành công, rồi được chuyển tới trang chi tiết của người dùng vừa tạo với dữ liệu và vai trò đúng như đã nhập.
result: pass

### 4. Sửa người dùng hiện có
expected: Mở `/admin/users/:login/edit`. Bạn phải thấy cùng form 2 cột như trang tạo mới. Sửa trường hồ sơ hoặc vai trò rồi lưu thì trang chi tiết phải phản ánh giá trị mới. Nếu bấm hủy, hệ thống phải quay lại trang chi tiết mà không áp dụng thay đổi.
result: pass

### 5. Quản lý trạng thái và hành động an toàn từ danh sách
expected: Tại danh sách người dùng, thao tác kích hoạt hoặc khóa phải cập nhật trạng thái người dùng và hiện phản hồi thành công. Thao tác xóa phải có bước xác nhận. Với chính tài khoản admin hiện tại, các hành động tự khóa hoặc tự xóa phải bị vô hiệu hóa.
result: pass

### 6. Cấp ROLE_ADMIN làm thay đổi quyền truy cập thực tế
expected: Nếu cấp `ROLE_ADMIN` cho một người dùng chưa có quyền admin rồi đăng nhập bằng chính tài khoản đó, người dùng phải truy cập được `/admin/users`.
result: pass

### 7. Thu hồi ROLE_ADMIN làm mất quyền truy cập thực tế
expected: Nếu thu hồi `ROLE_ADMIN` khỏi một người dùng đang có quyền admin rồi đăng nhập bằng chính tài khoản đó, người dùng phải bị chặn khỏi `/admin/users` và bị điều hướng tới `/accessdenied`.
result: pass

## Summary

total: 7
passed: 7
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps
