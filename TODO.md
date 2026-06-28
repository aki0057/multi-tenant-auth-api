# TODO

各レイヤーで作成したスタブ（メソッド・DomainObject / ValueObject）を、該当する層の章に記録する。
新規作成時は空のチェックボックスで追記し、実装が完了したらチェックを埋める。
## API (presentation)

- [x] AuthController#login
- [x] JwtAuthenticationFilter#doFilterInternal

## Service (application)

- [x] AuthService#login(LoginCommand) の JWT アクセストークン発行

## DomainObject (domain)
- [x] User#authenticate

## ValueObject (domain)
- [ ] Email
- [ ] Password
- [ ] RawPassword
- [ ] Role
- [x] TenantCode
- [x] TenantId
- [x] UserId

## Repository (domain)
- [ ] findByTenantCodeAndEmail

## Port (domain)
- [x] AccessTokenProvider#issue
- [x] PasswordVerifier#matches
- [x] AccessTokenVerifier#verify

## infrastructure.mapper

- [x] UserMapper#toDomain(UserJpaEntity)

## infrastructure.security

- [x] StubAccessTokenProvider を jjwt 実装へ置換
- [x] PasswordEncoderVerifier
- [x] StubAccessTokenVerifier を JwtAccessTokenVerifier へ置換
