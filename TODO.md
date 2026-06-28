# TODO

各レイヤーで作成したスタブ（メソッド・DomainObject / ValueObject）を、該当する層の章に記録する。
新規作成時は空のチェックボックスで追記し、実装が完了したらチェックを埋める。
## API (presentation)

- [x] AuthController#login

## Service (application)

- [x] AuthService#login(LoginCommand) の JWT アクセストークン発行

## DomainObject (domain)
- [ ] User

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
- [ ] AccessTokenProvider#issue

## infrastructure.mapper

- [x] UserMapper#toDomain(UserJpaEntity)

## infrastructure.security

- [ ] StubAccessTokenProvider を jjwt 実装へ置換
