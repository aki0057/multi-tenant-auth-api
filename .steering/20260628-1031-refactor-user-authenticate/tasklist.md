# tasklist

## 前提確認・TODO.md 準備

- [x] `User#authenticate` が TODO.md の DomainObject 章に存在しないため、空のチェックボックスで `User#authenticate` を追記する

## 新規ドメイン成果物の作成

- [x] `domain/service/PasswordVerifier.java` を新規作成する（`boolean matches(RawPassword rawPassword, PasswordHash passwordHash)` を持つインターフェース・Spring 非依存）
- [x] `domain/exception/AuthenticationFailedException.java` を新規作成する（`RuntimeException` 継承・引数なしコンストラクタ・Spring 非依存）
- [x] 各ファイルに Javadoc を記載する

## User ドメインオブジェクトへの authenticate 追加（本タスクの主軸）

- [x] `domain/model/User.java`（record）に `authenticate(RawPassword rawPassword, PasswordVerifier passwordVerifier): void` を instance method として追加・実装する（3 検証をこの順で行い、いずれか失敗で `AuthenticationFailedException` を throw する: ①`userIdIsActive` 確認、②`tenantIdIsActive` 確認、③`passwordVerifier.matches(rawPassword, passwordHash)` 確認）
- [x] `authenticate` メソッドに Javadoc を記載する
- [x] `UserTest.java` を新規作成し、正常系テストを記載する（`authenticate_success`: 全フラグ有効・verifier が true → 例外なし）
- [x] `UserTest.java` に異常系テストを記載する（`authenticate_userInactive`: userIdIsActive=false → `AuthenticationFailedException`、`authenticate_tenantInactive`: tenantIdIsActive=false → `AuthenticationFailedException`、`authenticate_wrongPassword`: verifier が false → `AuthenticationFailedException`）

## infrastructure アダプタの作成

- [x] `infrastructure/security/PasswordEncoderVerifier.java` を新規作成する（`@Component`・`@RequiredArgsConstructor`・既存 `PasswordEncoder` Bean をフィールドに持ち `matches` で `passwordEncoder.matches(rawPassword.value(), passwordHash.value())` を返す）
- [x] Javadoc を記載する

## AuthService のリファクタリング

- [x] `AuthService.java` のフィールド `PasswordEncoder passwordEncoder` を `PasswordVerifier passwordVerifier` へ差し替える（import も更新）
- [x] `AuthService.java` の 3 つの if-throw ブロックを削除し、`user.authenticate(rawPassword, passwordVerifier)` の呼び出しと `AuthenticationFailedException` を `BadCredentialsException("Invalid credentials")` へ変換する try-catch へ置換する
- [x] `AuthService.java` の Javadoc（`@throws` 等）を更新する

## AuthServiceTest の修正

- [x] `AuthServiceTest.java` の `@Mock PasswordEncoder passwordEncoder` を `@Mock PasswordVerifier passwordVerifier` へ差し替える（import も更新）
- [x] `login_success` テストで `when(passwordVerifier.matches(any(), any())).thenReturn(true)` へ更新する
- [x] `login_wrongPassword` テストで `when(passwordVerifier.matches(any(), any())).thenReturn(false)` へ更新する（inactive 系 2 テストは verifier をスタブしない）

## TODO.md の最終更新

- [x] `PasswordVerifier#matches` を TODO.md の Port 章に追記し、本タスクで完成させるためチェック済み（`[x]`）にする
- [x] `PasswordEncoderVerifier` を TODO.md の infrastructure.security 章に追記し、本タスクで完成させるためチェック済み（`[x]`）にする
- [x] `User#authenticate` の TODO.md チェックボックスを埋める（完了にする）
