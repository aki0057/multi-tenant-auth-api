# TODO

各レイヤーで作成したスタブ（メソッド・DomainObject / ValueObject）を、該当する層の章に記録する。
新規作成時は空のチェックボックスで追記し、実装が完了したらチェックを埋める。
## API (presentation)

- [x] AuthController#login
- [x] AuthController#refresh
- [x] JwtAuthenticationFilter#doFilterInternal

## Service (application)

- [x] AuthService#login(LoginCommand) の JWT アクセストークン発行
- [x] AuthService#refresh(RefreshCommand)

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
- [ ] findById（UserRepository）
- [ ] RefreshTokenRepository#findByTokenHash
- [ ] RefreshTokenRepository#save

## Port (domain)
- [x] AccessTokenProvider#issue
- [x] PasswordVerifier#matches
- [x] AccessTokenVerifier#verify
- [ ] RefreshTokenGenerator#generate
- [ ] RefreshTokenHasher#hash
- [ ] RefreshTokenExpirationPolicy#expiration

## infrastructure.mapper

- [x] UserMapper#toDomain(UserJpaEntity)

## infrastructure.security

- [x] StubAccessTokenProvider を jjwt 実装へ置換
- [x] PasswordEncoderVerifier
- [x] StubAccessTokenVerifier を JwtAccessTokenVerifier へ置換
- [ ] StubRefreshTokenGenerator を実装へ置換
- [ ] StubRefreshTokenHasher を実装へ置換
- [ ] StubRefreshTokenExpirationPolicy を実装へ置換

## infrastructure.persistence

- [ ] StubRefreshTokenRepository を実装へ置換
- [ ] UserRepositoryImpl#findById を実装
