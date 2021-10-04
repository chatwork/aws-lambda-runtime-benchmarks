terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 3.44"
    }
  }

  required_version = ">= 0.14.9"

  backend "local" {
    path = "terraform.tfstate"
  }
}

provider "aws" {
  #  profile = "${var.aws_profile}"
  region = "ap-northeast-1"
}
