readme.md

solo_master.c 의 온습도 함수가 작동하지 않을 땐 
reader_master 와 writer_dht11 둘을 동시에 실행시키면 같은 결과를 낼 수 있다.

writer_dht11는 온습도 모듈을 작동시켜 받아온 정보를 mydht 파일에 저장하고, reader_master
가 필요시 읽어가는 형태이다.

maker는 패스워드를 생성하여 mypass 파일에 저장한다. reader_master나 solo_master가 필요시 읽는다.

*사용 오픈소스

lirc-0.9.4d