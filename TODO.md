# TODO

各レイヤーで作成したスタブ（メソッド・DomainObject / ValueObject）を、該当する層の章に記録する。
新規作成時は空のチェックボックスで追記し、実装が完了したらチェックを埋める。
## API (presentation)

- [x] AuthController#login
- [x] AuthController#refresh
- [x] AuthController#logout
- [x] JwtAuthenticationFilter#doFilterInternal
- [x] UserController#getMe
- [x] AdminController#listTenantUsers

## Service (application)

- [x] AuthService#login(LoginCommand) の JWT アクセストークン＋リフレッシュトークン発行
- [x] AuthService#refresh(RefreshCommand)
- [x] AuthService#logout(LogoutCommand)
- [x] UserService#getMe(GetMeCommand)
- [ ] UserService#listTenantUsers(ListTenantUsersCommand)

## DomainObject (domain)
- [x] User#authenticate
- [x] RefreshToken
- [x] User#isActive
- [x] RefreshToken#isValid

## ValueObject (domain)
- [x] Email
- [x] PasswordHash
- [x] RawPassword
- [x] Role
- [x] TenantCode
- [x] TenantId
- [x] UserId
- [x] RefreshTokenId
- [x] TokenHash
- [x] RawRefreshToken
- [x] RawRefreshToken#toString
- [x] RawPassword#toString

## Repository (domain)
- [x] findByTenantCodeAndEmail
- [x] findById（UserRepository）
- [x] RefreshTokenRepository#findByTokenHash
- [x] RefreshTokenRepository#save

## Port (domain)
- [x] AccessTokenProvider#issue
- [x] PasswordVerifier#matches
- [x] AccessTokenVerifier#verify
- [x] RefreshTokenGenerator#generate
- [x] RefreshTokenHasher#hash
- [x] RefreshTokenExpirationPolicy#expiration

## infrastructure.mapper

- [x] UserMapper#toDomain(UserJpaEntity)
- [x] RefreshTokenMapper#toDomain(RefreshTokenJpaEntity)
- [x] RefreshTokenMapper#toEntity(RefreshToken, TenantJpaEntity, UserJpaEntity)

## infrastructure.security

- [x] StubAccessTokenProvider を jjwt 実装へ置換
- [x] PasswordEncoderVerifier
- [x] StubAccessTokenVerifier を JwtAccessTokenVerifier へ置換
- [x] StubRefreshTokenGenerator を実装へ置換
- [x] StubRefreshTokenHasher を実装へ置換
- [x] StubRefreshTokenExpirationPolicy を実装へ置換

## infrastructure.persistence

- [x] StubRefreshTokenRepository を実装へ置換
- [x] UserRepositoryImpl#findById を実装
