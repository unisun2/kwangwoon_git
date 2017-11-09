//master


#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <unistd.h>
#include <signal.h>
#include <python2.7/Python.h>
#include <wiringPi.h>
#include <stdint.h>
#include <sys/time.h>
#include <time.h>

#define BUFFSIZE 1024
#define PORTNO 41009
#define MAXTIMINGS 85
#define DHTPIN 7
#define LED_START 4
#define LED_CONN 5

int socket_fd, client_fd;
pid_t pid = 41009;

void handler(int signo);
void read_dht11_dat(char *income);

int main() {

	struct sockaddr_in server_addr, client_addr;
	clock_t start, end;
	int len, len_out, opt, random = 0, send_check = 1;
	char buf[BUFFSIZE];
	char ret_val[100] = { 0, };
	int cur_LightsOnOff = 0;

	int authorization_successful = 0;

	char pass_reader[80] = { 0, };
	char temp_pass_save[80] = { 0 };

	FILE *fptr = 0;


	// 소켓 연결 실패
	while(ret_val[0] ==0){

		read_dht11_dat(ret_val);
		delay(2000);
	}


	
	if ((socket_fd = socket(PF_INET, SOCK_STREAM, 0)) < 0) {
		printf("Server: Can't open stream socket.\n");
		return 0;
	}

	setsockopt(socket_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
	bzero((char *)&server_addr, sizeof(server_addr));

	server_addr.sin_family = AF_INET;
	server_addr.sin_addr.s_addr = htonl(INADDR_ANY);
	server_addr.sin_port = htons(PORTNO);

	// 소켓 바인딩 실패
	if (bind(socket_fd, (struct sockaddr *)&server_addr, sizeof(server_addr)) < 0) {
		printf("Server: Can't bind local address.\n");
		return 0;
	}

	// 대기
	listen(socket_fd, 5);

	while (1) {
		//digitalWrite(LED_START, 1);
		//digitalWrite(LED_CONN, 0);

		len = sizeof(client_addr);
		client_fd = accept(socket_fd, (struct sockaddr *)&client_addr, &len);

		//digitalWrite(LED_START, 0);
		//digitalWrite(LED_CONN, 1);

		if (client_fd < 0) {
			printf("Server: accept failed.\n");
			return 0;
		}

		// 포크
		if ((pid = fork()) == 0) {
			while ((len_out = read(client_fd, buf, BUFFSIZE)) > 0) {
				
				// 60초 후 자동종료
				// alarm(60);

				printf("%s, %d\n", buf, len_out);

				// 패스워드 받아서 확인작업.
				if (strncmp(buf, "Pass_:", 6) == 0) {

					if (authorization_successful == 0) {	// 이미 인증작업이 끝났으면 다시 할 필요 없다.

						memset(temp_pass_save, 0, 80);

						for (int k = 6; buf[k]; k++) {		// temp_pass_save 에 받은 패스워드 저장
							temp_pass_save[k - 6] = buf[k];
						}


						
						fptr = fopen("./mypass", "rb");	// 파일 열어서 한줄씩 비교 시작

						while (0 < fscanf(fptr, "%s", pass_reader)) {

							if (strncmp(pass_reader, temp_pass_save, 64) == 0) { // 동일하다면

								authorization_successful = 1; // 인증 완료
								printf("authorization_successful = 1\n");

								write(client_fd, "password_ok\r\n" , strlen("password_ok\r\n"));

							}

							printf("fscanf : ");
							printf("%s", pass_reader);

						}

						fclose(fptr);

						if (authorization_successful == 0) {
							printf("authorization_failed\n");
							write(client_fd, "password_not\r\n", strlen("password_not\r\n"));
						}


					} // 인증변수 확인
				} // Pass_: 으로 시작
				
				// 이 부분 뒤로는 패스워드 인증을 받은 후에 갈 수 있다. 없으면 메시지를 띄우며 명령 무시
				if (authorization_successful == 0) {		
					printf("need authorization\n");
					continue;

				}



				// 받은 메시지가 "UP" 일때
				if (strcmp(buf, "UP") == 0) {

					printf("up\n");
					cur_LightsOnOff = 1; // 켜져 있는것을 표시
					// 이 부분에 슬레이브에 메시지 전달
					system("irsend SEND_ONCE test11 KEY_1");

				}

				else if (strcmp(buf, "DOWN") == 0) {

					printf("down\n");
					cur_LightsOnOff = 0; // 끈다.
					system("irsend SEND_ONCE test11 KEY_3");

				}

				// 온습도 측정하고 보낼 준비
				else if (strcmp(buf, "CIRCUMS") == 0) {

					delay(250);
					read_dht11_dat(ret_val);
					printf("%s in read_dht11_dat \n", ret_val);
					strcat(ret_val,"\r\n");

					send_check = 0;
				}

				else if (strcmp(buf, "ENDUP") == 0) {	// 종료
					kill(getpid(), SIGALRM);
				}

				// send_check 가 0 이면 클라이언트에게 온습도 정보 보냄
				if (send_check == 0) {
					
					write(client_fd, ret_val, strlen(ret_val));
					
					send_check = 1;
				}
				
				if (cur_LightsOnOff == 0) {		// 0, 꺼져있을시
					write(client_fd, "0\r\n", strlen("0\r\n")); // 아니면 ACK 정보 보냄
				}
				else if (cur_LightsOnOff == 1) {		// 0, 꺼져있을시
					write(client_fd, "1\r\n", strlen("1\r\n")); // 아니면 ACK 정보 보냄
				}
					

				send_check = 1;
				// send_check 를 1로 해놓으면 

				if (strcmp(buf, "CONNECTION_STABLE_STATE") == 0) {		// CONNECTION_STABLE_STATE 받으면 RECEIVE_SUCCESSFULLY 회신

					memset(buf, 0, BUFFSIZE);
					strcat(buf, "RECEIVE_SUCCESSFULLY");
					strcat(buf, "\r\n");
					write(client_fd, buf, strlen(buf));
				}

				if(ret_val[0] == 0)			// 현재 온도값이 존재하지 않으면
					read_dht11_dat(ret_val); // 한번은 측정

				memset(buf, 0, BUFFSIZE);
			}
		}

		else {
			// 패런츠는 아무것도 안함
		}

		close(client_fd);

		wait(NULL);
	}

	close(socket_fd);
	return 0;
}

void handler(int signo) {
	if (signo == SIGALRM || (signo == SIGINT && pid == 0)) {
		printf("Close Old One \n");
		close(client_fd);
		exit(0);
	}

	else if (signo == SIGINT && pid != 0) {
		kill(pid, SIGINT);

		digitalWrite(LED_CONN, 0);
		digitalWrite(LED_START, 0);

		close(socket_fd);

		exit(0);
	}
}

// 온습도 측정 데이터를 가져옴
void read_dht11_dat(char *income) {
	FILE *fptr = 0;
	if(fptr = fopen("./mydht", "rb")){
		fscanf(fptr, "%s", income);
		fclose(fptr);
	}
}
