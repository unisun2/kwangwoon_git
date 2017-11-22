#include <stdio.h>
#include <string.h>
#include <openssl/sha.h>
#include <stdlib.h>
int simpleSHA256(void* input, unsigned long length, unsigned char* md)
{
    SHA256_CTX context;
    if(!SHA256_Init(&context))
        return 0;

    if(!SHA256_Update(&context, (unsigned char*)input, length))
        return 0;

    if(!SHA256_Final(md, &context))
        return 0;

    return 1;
}

int main(){
	int i = 0;
	unsigned char hello[] = "hello";
	unsigned char zeroo[] = "0000";
	unsigned char  hash[64];
	unsigned char hash2[64];
	char mdstring[SHA256_DIGEST_LENGTH*2+1];
	char mdstring2[SHA256_DIGEST_LENGTH*2+1];

	FILE * fptr;

	
	printf("%d\n", SHA256_DIGEST_LENGTH);

	simpleSHA256(hello,5,hash);

	SHA256(zeroo,4,hash2);

	printf("%s\n", hash);
	printf("%s\n", hash2);

	printf("\n");
	while(hash[i]){
		printf("%d..",hash2[i]);
		i++;
	}
	
	for(i = 0; i < SHA256_DIGEST_LENGTH; i++){
		sprintf(&mdstring[i*2], "%02x", (unsigned int)hash[i]);
		sprintf(&mdstring2[i*2], "%02x", (unsigned int)hash2[i]);
		
	}
	printf("\nSHA256.... : %s\n", mdstring);

	fptr = fopen("./mypass","wb");

	fprintf(fptr, "%s %s",mdstring,mdstring2);

	fclose(fptr);
return 0;
}
