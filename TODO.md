# TODO

各レイヤーで作成したスタブ（メソッド・DomainObject / ValueObject）を、該当する層の章に記録する。
新規作成時は空のチェックボックスで追記し、実装が完了したらチェックを埋める。
## API (presentation)

- [x] AuthController#login

## Service (application)

- [ ] AuthService#login(LoginCommand) の JWT アクセストークン発行

## DomainObject (domain)
- [ ] User

## ValueObject (domain)
- [ ] Email
- [ ] Password
- [ ] RawPassword
- [ ] Role
- [ ] TenantCode
- [ ] UserId

## Repository (domain)
- [ ] findByTenantCodeAndEmail
## infrastructure.mapper

- [x] UserMapper#toDomain(UserJpaEntity)
