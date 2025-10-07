# planning-poker

Free `Simple` and `easy` planning poker web application.

run and usage

Run the application locally

```shell
./mvnw spring-boot:run
```
or

Download the `jar` file from [github packages]() and run it like:
```shell
java -jar planning-poker-1.0.0.jar
```

or

Pull the docker image and Run

```shell
docker pull ghcr.io/samitkumarpatel/planning-poker:1.0.0
docker run --rm -p 8080:8080
```

Access the application by accessing [http://localhost:8080](http://localhost:8080).
- Create a room by giving the card details you want.
- Share the generated URI/URL with the participant.
- They need to give a name and choose either `participant` or `observer`
- As a `Participant` they can vote.
- As a `Observer` you can revel the vote , so that other cab see the vote details on their screen.
- As a `Observer` you can do more then a `Participant`.
- More to come ...